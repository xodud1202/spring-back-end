package com.xodud1202.springbackend.security.news;

import java.io.Console;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 운영자가 별도 승인 후 직접 실행하는 대화형 해시 파일 작성 도구입니다.
public final class NewsOAuthPasswordConsole {
    private NewsOAuthPasswordConsole() {}
    // 비밀번호는 콘솔에서만 읽고 명령행 인수/환경변수/출력으로 전달하지 않습니다.
    public static void main(String[] args) throws Exception {
        Console console=System.console();
        if(console==null || args.length!=1) throw new IllegalArgumentException("실제 터미널에서 새 해시 파일 경로 1개를 지정하세요.");
        Path target=Path.of(args[0]).toAbsolutePath();
        if(Files.exists(target)) throw new IllegalArgumentException("기존 파일은 덮어쓰지 않습니다. 새 경로를 지정하세요.");
        char[] password=console.readPassword("새로운 강한 비밀값(20자 이상): ");
        char[] confirm=console.readPassword("다시 입력: ");
        try {
            if(password==null || confirm==null || !Arrays.equals(password,confirm)
                    || password.length<20 || new String(password).getBytes(StandardCharsets.UTF_8).length>72)
                throw new IllegalArgumentException("입력 불일치 또는 길이 조건 위반입니다.");
            // 생성 시부터 소유자만 읽고 쓸 수 있는 권한을 설정합니다.
            FileStore store=Files.getFileStore(target.getParent());
            if(store.supportsFileAttributeView("posix")) {
                Files.createFile(target,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            } else if(store.supportsFileAttributeView("acl")) {
                UserPrincipal user=target.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
                AclEntry entry=AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(user)
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();
                FileAttribute<List<AclEntry>> attr=new FileAttribute<>() {
                    public String name(){return "acl:acl";}
                    public List<AclEntry> value(){return List.of(entry);}
                };
                Files.createFile(target,attr);
            } else throw new IllegalStateException("안전한 파일 권한을 설정할 수 없습니다.");
            Files.writeString(target,new BCryptPasswordEncoder(12).encode(new String(password)),StandardOpenOption.WRITE);
            console.printf("해시 파일을 저장했습니다. 평문은 출력하지 않았습니다.%n");
        } finally {
            if(password!=null) Arrays.fill(password,'\0');
            if(confirm!=null) Arrays.fill(confirm,'\0');
        }
    }
}