package msacoffeechainsample;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Mypage_table")
public class Mypage {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Long orderId;
        private String orderStatus;
        private Long productId;
        private String productStatus;
        private String productName;
        private Integer qty;
        private Long customerId;
        private String customerLevel;

        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }

        public Long getOrderId() {
            return orderId;
        }
        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public String getOrderStatus() {
            return orderStatus;
        }
        public void setOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
        }

        public Long getProductId() {
            return productId;
        }
        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductStatus() {
            return productStatus;
        }
        public void setProductStatus(String productStatus) {
            this.productStatus = productStatus;
        }

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

        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }

        public String getCustomerLevel() { return customerLevel; }
        public void setCustomerLevel(String customerLevel) { this.customerLevel = customerLevel; }
}
