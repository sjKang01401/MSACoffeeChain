package msacoffeechainsample;

import javax.persistence.*;

import msacoffeechainsample.external.Product;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long productId;
    private String productName;
    private Integer qty;
    private String status;
    private Long customerId;
    private String customerLevel;

    @PrePersist
    public void onPrePersist() {

        msacoffeechainsample.external.CustomerInfo customerInfo = new msacoffeechainsample.external.CustomerInfo();
        customerInfo.setId(this.getCustomerId());

        // req/res
        String customerLevel = OrderApplication.applicationContext.getBean(msacoffeechainsample.external.CustomerInfoService.class)
                                .checkLevel(customerInfo.getId());

        // update customer level
        this.setCustomerLevel(customerLevel);

        // Status 변화
        this.setStatus("Requested");
    }

    @PostPersist
    public void onPostPersist(){

        // Event 객체 생성
        Ordered ordered = new Ordered();

        // Aggregate 값을 Event 객체로 복사
        BeanUtils.copyProperties(this, ordered);

        // pub/sub
        ordered.publishAfterCommit();
    }

    @PreUpdate
    public void onPreUpdate(){

        // 주문 취소의 경우
        if (this.getStatus().equals("Canceled")) {

            // Event 객체 생성
            OrderCanceled orderCanceled = new OrderCanceled();

            // Aggregate 값을 Event 객체로 복사
            BeanUtils.copyProperties(this, orderCanceled);

            msacoffeechainsample.external.Product product = new msacoffeechainsample.external.Product();
            product.setId(orderCanceled.getProductId());
            product.setOrderId(orderCanceled.getId());
            product.setProductName(orderCanceled.getProductName());
            product.setStatus(orderCanceled.getStatus());
            product.setQty(orderCanceled.getQty());

            // req/res
            OrderApplication.applicationContext.getBean(msacoffeechainsample.external.ProductService.class)
                    .cancel(product.getId(), product);
        }

        // Event 객체 생성
        StatusUpdated statusUpdated = new StatusUpdated();

        // Aggregate 값을 Event 객체로 복사
        BeanUtils.copyProperties(this, statusUpdated);

        // 주문 취소 시 qty 마이너스
        if (this.getStatus().equals("Canceled")) { statusUpdated.setQty(statusUpdated.getQty() * -1 ); }

        System.out.println("StatusUpdate >> " + statusUpdated.toJson());

        // pub/sub
        statusUpdated.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQty() {
        return qty;
    }
    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerLevel() { return customerLevel; }
    public void setCustomerLevel(String customerLevel) { this.customerLevel = customerLevel; }
}
