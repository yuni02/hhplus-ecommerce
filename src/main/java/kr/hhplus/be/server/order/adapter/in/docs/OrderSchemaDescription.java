package kr.hhplus.be.server.order.adapter.in.docs;

public interface OrderSchemaDescription {

    String userId = "사용자 ID";
    String productId = "상품 ID";
    String orderId = "주문 ID";
    String couponId = "쿠폰 ID";
    String quantity = "수량";
    String totalAmount = "주문 금액";
    String discountAmount = "할인 금액";
    String paidAmount = "지불 금액";
    String createAt = "주문 생성일";
    String orderProduct = "주문 상품";

} 