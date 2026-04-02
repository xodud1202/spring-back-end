package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartItemVO;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsGroupItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsImageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishGoodsItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
// 상품 이미지 URL 보정 공통 기능을 제공합니다.
public class GoodsImageService {
	private final FtpFileService ftpFileService;

	// 관리자 상품 목록에 이미지 URL을 세팅합니다.
	public void applyAdminGoodsListImageUrls(List<GoodsVO> list) {
		// 목록이 없으면 바로 종료합니다.
		if (list == null || list.isEmpty()) {
			return;
		}
		for (GoodsVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 카테고리 상품 목록에 이미지 URL을 세팅합니다.
	public void applyCategoryGoodsImageUrls(List<CategoryGoodsVO> list) {
		// 목록이 없으면 바로 종료합니다.
		if (list == null || list.isEmpty()) {
			return;
		}
		for (CategoryGoodsVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 쇼핑몰 카테고리 상품 목록에 이미지 URL을 세팅합니다.
	public void applyShopCategoryGoodsImageUrls(List<ShopCategoryGoodsItemVO> list) {
		// 목록이 없으면 바로 종료합니다.
		if (list == null || list.isEmpty()) {
			return;
		}
		for (ShopCategoryGoodsItemVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
			item.setSecondaryImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getSecondaryImgPath()));
		}
	}

	// 쇼핑몰 마이페이지 위시리스트 상품 목록에 이미지 URL을 세팅합니다.
	public void applyShopMypageWishGoodsImageUrls(List<ShopMypageWishGoodsItemVO> list) {
		// 목록이 없으면 바로 종료합니다.
		if (list == null || list.isEmpty()) {
			return;
		}
		for (ShopMypageWishGoodsItemVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 상품상세 이미지 목록의 표시용 URL을 보정합니다.
	public void applyShopGoodsImageUrlList(List<ShopGoodsImageVO> imageList) {
		// 목록이 없으면 바로 종료합니다.
		if (imageList == null || imageList.isEmpty()) {
			return;
		}
		for (ShopGoodsImageVO item : imageList) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 그룹상품 목록의 대표 이미지 URL을 보정합니다.
	public void applyShopGoodsGroupItemImageUrlList(List<ShopGoodsGroupItemVO> groupGoodsList) {
		// 목록이 없으면 바로 종료합니다.
		if (groupGoodsList == null || groupGoodsList.isEmpty()) {
			return;
		}
		for (ShopGoodsGroupItemVO item : groupGoodsList) {
			if (item == null) {
				continue;
			}
			item.setFirstImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getFirstImgPath()));
		}
	}

	// 쇼핑몰 마이페이지 주문내역 주문상세 목록에 이미지 URL을 세팅합니다.
	public void applyShopMypageOrderDetailImageUrls(List<ShopMypageOrderDetailItemVO> list) {
		// 목록이 없으면 바로 종료합니다.
		if (list == null || list.isEmpty()) {
			return;
		}
		for (ShopMypageOrderDetailItemVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 쇼핑몰 장바구니 상품 목록에 이미지 URL을 세팅합니다.
	public void applyShopCartItemImageUrls(List<ShopCartItemVO> list) {
		// 목록이 없으면 바로 종료합니다.
		if (list == null || list.isEmpty()) {
			return;
		}
		for (ShopCartItemVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 관리자 상품 이미지 목록에 이미지 URL을 세팅합니다.
	public void applyAdminGoodsImageUrls(List<GoodsImageVO> list, String goodsId) {
		// 목록이 없으면 바로 종료합니다.
		if (list == null || list.isEmpty()) {
			return;
		}
		for (GoodsImageVO item : list) {
			if (item == null) {
				continue;
			}
			String resolvedGoodsId = isBlank(item.getGoodsId()) ? goodsId : item.getGoodsId();
			item.setImgUrl(resolveShopGoodsImageUrl(resolvedGoodsId, item.getImgPath()));
		}
	}

	// 쇼핑몰 상품 이미지 경로를 UI 조회용 URL로 변환합니다.
	public String resolveShopGoodsImageUrl(String goodsId, String imgPath) {
		// 이미지 경로가 없으면 빈 문자열을 반환합니다.
		if (isBlank(imgPath)) {
			return "";
		}
		// 절대 URL은 그대로 반환합니다.
		if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
			return imgPath;
		}
		// 파일명만 있는 경우 FTP 규칙으로 URL을 생성합니다.
		return ftpFileService.buildGoodsImageUrl(goodsId, imgPath);
	}

	// 문자열이 null 또는 공백인지 확인합니다.
	private boolean isBlank(String value) {
		// null이거나 trim 결과가 비어 있으면 공백으로 판단합니다.
		return value == null || value.trim().isEmpty();
	}
}
