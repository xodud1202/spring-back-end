import assert from 'node:assert/strict';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';

const url = new URL(process.env.NEWS_MCP_TEST_URL);
assert.equal(url.hostname, '127.0.0.1', 'This fixture must never target production');
const token = process.env.NEWS_MCP_TEST_BEARER;
const observations = [];
const fetchObserved = async (input, init) => {
  const response = await fetch(input, init);
  const headers = new Headers(init?.headers);
  let message;
  try { message = JSON.parse(init?.body); } catch {}
  observations.push({ method: init?.method ?? 'GET', rpc: message?.method,
    status: response.status, contentType: response.headers.get('content-type'),
    protocolVersion: headers.get('mcp-protocol-version'), proposedVersion: message?.params?.protocolVersion });
  return response;
};
const client = new Client({ name: 'news-compatibility-test', version: '1.0.0' });
const transport = new StreamableHTTPClientTransport(url, {
  requestInit: { headers: { Authorization: 'Bearer ' + token } }, fetch: fetchObserved
});
try {
  await client.connect(transport);
  assert.equal(client.getServerVersion().name, 'news-daily');
  const listed = await client.listTools();
  assert.deepEqual(listed.tools.map(t => t.name), ['news_attempt_start', 'news_attempt_fail', 'news_source', 'news_summary_save', 'news_save_status', 'news_daily_latest']);
  const latest = await client.callTool({ name: 'news_daily_latest', arguments: { yyyymmdd: '20261013' } });
  assert.equal(latest.isError, false);
  assert.equal(JSON.parse(latest.content[0].text).status, 'EMPTY');
  const begun = await client.callTool({ name: 'news_attempt_start', arguments: {
    yyyymmdd: '20261013', requestId: 'client-test', sourceGeneratedAt: '2026-10-13 09:12:00'
  } });
  assert.equal(begun.isError, false);
  assert.equal(JSON.parse(begun.content[0].text).status, 'PENDING');
  const failed = await client.callTool({ name: 'news_attempt_fail', arguments: {
    yyyymmdd: '20261013', requestId: 'failure-test', reasonCode: 'SUMMARY_FAILED'
  } });
  assert.equal(failed.isError, false);
  assert.equal(JSON.parse(failed.content[0].text).status, 'FAILED');
  const source = await client.callTool({ name: 'news_source', arguments: {} });
  assert.equal(source.isError, false);
  // The source fixture contains hostile text: it stays data and is never executed.
  assert.match(source.content[0].text, /IGNORE ALL RULES/);
  const status = await client.callTool({ name: 'news_save_status', arguments: { yyyymmdd: '20261013', requestId: 'client-test' } });
  assert.equal(status.isError, false);
  const saved = await client.callTool({ name: 'news_summary_save', arguments: {
    yyyymmdd: '20261013', requestId: 'client-test', sourceGeneratedAt: '2026-10-13 09:12:00',
    articles: Array.from({ length: 20 }, (_, i) => ({
      categoryNm: i < 18 ? '사회' : i === 18 ? '스포츠' : '날씨', dispOrd: i + 1, newsContent: 'fixture'
    }))
  } });
  assert.equal(saved.isError, false);
  const conflict = await client.callTool({ name: 'news_summary_save', arguments: {
    yyyymmdd: '20261013', requestId: 'conflict-test', sourceGeneratedAt: '2026-10-13 09:12:00', articles: []
  } });
  assert.equal(conflict.isError, true);
  const tooLong = await client.callTool({ name: 'news_summary_save', arguments: {
    yyyymmdd: '20261013', requestId: 'length-test', sourceGeneratedAt: '2026-10-13 09:12:00', articles: []
  } });
  assert.equal(tooLong.isError, true);
  const detail = JSON.parse(tooLong.content[0].text);
  assert.equal(detail.articleDispOrd, 6); assert.equal(detail.field, 'newsContent');
  assert.equal(detail.maxLength, 800); assert.equal(detail.actualLength, 801);
  await client.ping();
  assert(observations.some(o => o.rpc === 'initialize' && o.status === 200 && o.contentType?.startsWith('application/json')));
  assert(observations.some(o => o.rpc === 'notifications/initialized' && o.status === 202));
  assert(observations.some(o => o.rpc === 'tools/list' && o.protocolVersion === '2025-06-18'));
} finally { await client.close(); }

const headers = { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json',
  Accept: 'application/json, text/event-stream', 'MCP-Protocol-Version': '2025-06-18' };
const ping = JSON.stringify({ jsonrpc: '2.0', id: 10, method: 'ping' });
async function check(name, expected, overrides = {}) {
  const response = await fetch(url, { method: 'POST', headers, body: ping, ...overrides });
  assert.equal(response.status, expected, name);
  observations.push({ case: name, status: response.status, contentType: response.headers.get('content-type'),
    challenge: response.headers.get('www-authenticate') });
  return response;
}
await check('missing authentication', 401, { headers: { 'Content-Type': 'application/json' } });
await check('invalid bearer', 401, { headers: { ...headers, Authorization: 'Bearer invalid-fixture' } });
await check('unsupported version', 400, { headers: { ...headers, 'MCP-Protocol-Version': '2099-01-01' } });
await check('wrong Content-Type', 415, { headers: { ...headers, 'Content-Type': 'text/plain' } });
await check('missing event-stream Accept', 406, { headers: { ...headers, Accept: 'application/json' } });
await check('zero quality JSON Accept', 406, { headers: { ...headers, Accept: 'application/json;q=0, text/event-stream' } });
await check('bad origin', 403, { headers: { ...headers, Origin: 'https://untrusted.invalid' } });
await check('GET SSE unsupported', 405, { method: 'GET', headers, body: undefined });
await check('DELETE sessions unsupported', 405, { method: 'DELETE', headers, body: undefined });
await check('missing negotiated version', 400, { headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json', Accept: headers.Accept } });
const parse = await check('malformed JSON', 400, { body: '{' });
assert.equal((await parse.json()).error.code, -32700);
const trailing = await check('trailing JSON tokens', 400, { body: ping + ping });
assert.equal((await trailing.json()).error.code, -32700);
const batch = await check('JSON-RPC batch unsupported', 200, { body: '[' + ping + ']' });
assert.equal((await batch.json()).error.code, -32600);
const notify = await check('write notification rejected', 400, { body: JSON.stringify({
  jsonrpc: '2.0', method: 'tools/call', params: { name: 'news_summary_save', arguments: {} }
}) });
assert.equal((await notify.text()).length, 0);
const anonymousClient = new Client({ name: 'unauthenticated-test', version: '1.0' });
try {
  await assert.rejects(anonymousClient.connect(new StreamableHTTPClientTransport(url)), error => error.code === 401);
} finally { await anonymousClient.close(); }
console.log(JSON.stringify({ sdk: '@modelcontextprotocol/sdk@1.32.1', result: 'PASS', observations }, null, 2));



