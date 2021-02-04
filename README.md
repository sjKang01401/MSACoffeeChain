![MSA_CoffeChain_Reserve_logo](https://user-images.githubusercontent.com/26760226/106830753-0819b580-66d2-11eb-91d6-17d2b81a9ba4.jpg)

# Coffee Chain *Reserve*
- [설계](#설계)
  1. [Event Storming 결과](#1.-event-storming-결과)
- [구현](#구현)
  1. [DDD의 구현](#ddd의-구현)
  2. [시나리오 #1 : countScore]()
  3. [시나리오 #2 : checkLevel]()
  4. [Gateway 적용](#gateway-적용)
  5. [Polyglot](#polyglot)
- [운영](#운영)
  1. [Deploy/ Pipeline](#deploy--pipeline)
  2. [Circuit Breaker](#circuit-breaker)
  3. [Autoscale](#autoscale)
  4. [Zero-downtime deploy](#zero-downtime-deploy)
  5. [Config map](#config-map)
  6. Self-healing (Liveness Probe)

# 설계

### 1. Event Storming 결과
- **고객 관리 서비스** 시나리오
   ```
   1. 주문을 완료하거나 주문을 취소하면 고객의 구매 횟수를 카운팅한다 (countScore)
   2. 다음 주문 시 고객의 구매 횟수를 바탕으로 고객의 등급을 산출한다 (checkLevel)
   ```
- **고객 관리 서비스**를 추가한 완성된 모형 (하단 노란색 박스)
![Modeling](https://user-images.githubusercontent.com/26760226/106832292-b6bef580-66d4-11eb-8285-01c070b1b9c7.png)

<br>

# 구현

### 1. DDD의 구현
``` java
@Entity
@Table(name="CustomerInfo_table")
public class CustomerInfo {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String name;
    private Integer score;
    private String level;

    @PrePersist
    public void onPrePersist()
    {
        this.setLevel("Bronze");
        this.setScore(0);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    ...
}
```
- 고객 정보 생성 <br>
![create_customer](https://user-images.githubusercontent.com/26760226/106834656-3222a600-66d9-11eb-97d3-88bb47a595db.png)

### 2. 시나리오 #1 : countScore
주문을 완료하거나 주문을 취소하면 고객의 구매 횟수를 카운팅한다

- 호출 : Order.java
``` java
@Entity
@Table(name="Order_table")
public class Order {

    @PreUpdate
    public void onPreUpdate() {

        ...

        // Event 객체 생성
        StatusUpdated statusUpdated = new StatusUpdated();

        // Aggregate 값을 Event 객체로 복사
        BeanUtils.copyProperties(this, statusUpdated);

        // 주문 취소 시 qty 차감
        if (this.getStatus().equals("Canceled")) {
             statusUpdated.setQty(statusUpdated.getQty() * -1 );
        }

        // pub/sub
        statusUpdated.publishAfterCommit();
    }
}
```
- 피호출 : PolicyHandler.java
``` java
@Service
public class PolicyHandler {

    @Autowired
    CustomerInfoRepository customerInfoRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverStatusUpdated_(@Payload StatusUpdated statusUpdated){

        if(statusUpdated.isMe()){

            // customer 정보 가져오기
            CustomerInfo customerInfo = customerInfoRepository.findById(statusUpdated.getCustomerId()).get();

            // 구매 횟수 counting
            customerInfo.setScore( customerInfo.getScore() + statusUpdated.getQty() );

            // customer 정보 저장
            customerInfoRepository.save(customerInfo);
        }
    }
}
```
- 주문 전 고객 정보 <br>
![1_get_customer](https://user-images.githubusercontent.com/26760226/106837784-6d73a380-66de-11eb-97be-ad7834caee32.png)

- 주문 <br>
![1_order](https://user-images.githubusercontent.com/26760226/106837077-af501a00-66dd-11eb-9f4d-084f8f711886.png)

- 주문 후 고객 정보 <br>
![1_customer_result](https://user-images.githubusercontent.com/26760226/106837088-b119dd80-66dd-11eb-85bd-6a26c165c0ae.png)

- 마이 페이지 <br>
![1_mypage](https://user-images.githubusercontent.com/26760226/106837075-aeb78380-66dd-11eb-825a-42ec1cc0fe8d.png)

### 3. 시나리오 #2 : checkLevel
다음 주문 시 고객의 구매 횟수를 바탕으로 고객의 등급을 산출한다

- 호출 : Order.java
``` java
@Entity
@Table(name="Order_table")
public class Order {

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
}
```

- 피호출 : CustomerController.java
``` java
@RestController
public class CustomerInfoController {

    @Autowired
    CustomerInfoRepository customerInfoRepository;

    @RequestMapping(method= RequestMethod.POST, path="/customers/{customerId}/level")
    public String checkLevel(@PathVariable("customerId") Long id) {

	Optional<CustomerInfo> customerOptional = customerInfoRepository.findById(id);

	if (customerOptional.isPresent()) {

                // customer 정보 가져오기
		CustomerInfo customerInfo = customerOptional.get();
		String customerLevel = "";

                // 구매 횟수를 바탕으로 레벨 계산
		switch (customerInfo.getScore() / 5) {
			case 0 : customerLevel = "Bronze";
				 break;
			case 1 : customerLevel = "Silver";
				 break;
                        case 2 : customerLevel = "Gold";
				 break;
			case 3 : customerLevel = "Platinum";
				 break;
			case 4 :
			default : customerLevel = "Diamond";
		}

                // customer 정보 저장
		customerInfo.setLevel(customerLevel);
		customerInfoRepository.save(customerInfo);

		return customerLevel;
	}

	return "Iron";
    }
}
```
- 주문 <br>
![2_order](https://user-images.githubusercontent.com/26760226/106837086-b0814700-66dd-11eb-95c6-9a7278ca2f7b.png)

- 주문 후 고객 정보 <br>
![2_customer_result](https://user-images.githubusercontent.com/26760226/106837080-af501a00-66dd-11eb-8517-9b7eb611eab3.png)

- 마이 페이지 <br>
![2_mypage](https://user-images.githubusercontent.com/26760226/106837081-afe8b080-66dd-11eb-9827-e1ca1708d790.png)

### 4. Gateway 적용
- application.xml <br>
![gateway](https://user-images.githubusercontent.com/26760226/106840485-9d717580-66e3-11eb-8f21-5a155b4b0e47.png)
- gateway 주소로 고객 정보를 조회하면 동일한 결과 <br>
![gateway_test](https://user-images.githubusercontent.com/26760226/106840712-08bb4780-66e4-11eb-8c13-dcdc098cb910.png)

### 5. Polyglot
- Customer 서비스는 다른 서비스와 달리 hsqldb 사용 <br>
![polyglot](https://user-images.githubusercontent.com/26760226/106841139-e118af00-66e4-11eb-86f7-874cfba18b48.png)

<br>

# 운영

### 1. Deploy/ Pipeline
![deploy_1](https://user-images.githubusercontent.com/26760226/106842169-d8c17380-66e6-11eb-9f36-9b439736a15b.png)
![deploy_2](https://user-images.githubusercontent.com/26760226/106842173-d95a0a00-66e6-11eb-9cb5-333cede5e24d.png)
![deploy_4](https://user-images.githubusercontent.com/26760226/106842347-3655c000-66e7-11eb-8633-7f8f2676b190.png)
![deploy_5](https://user-images.githubusercontent.com/26760226/106842176-da8b3700-66e6-11eb-88a3-51f9948b565d.png)

### 2. Circuit Breaker
- order의 application.xml <br>
![circuit_1](https://user-images.githubusercontent.com/26760226/106845626-ad428700-66ee-11eb-8ddf-8bb3f2a2f593.png)
- siege command
``` bash
siege -c10 -t60S -r10 -v --content-type "application/json" 'http://order:8080/orders POST {"customerId":1, "productName":"Americano", "qty":1}'
```
- siege 결과 <br>
![siege_result](https://user-images.githubusercontent.com/26760226/106845678-c9debf00-66ee-11eb-9b65-536446f3395a.png)
![siege_result_2](https://user-images.githubusercontent.com/26760226/106845681-ca775580-66ee-11eb-8a6d-40a92f34ba4d.png)

### 3. Autoscale
- order의 deployment.xml <br>
![autoscale_1](https://user-images.githubusercontent.com/26760226/106846489-4aea8600-66f0-11eb-90ce-ae30a30da9c7.png)
``` bash
kubectl autoscale deploy order --min=1 --max=10 --cpu-percent=15
```
- siege
``` bash
siege -c250 -t50S -r1000 -v --content-type "application/json" 'http://order:8080/orders POST {"customerId":1, "productName":"Americano", "qty":1}'
```
- 모니터링 결과
``` bash
kubectl get deploy order -w
```
![autoscale_2](https://user-images.githubusercontent.com/26760226/106846585-838a5f80-66f0-11eb-9538-ef1a7ca9f19f.png)

### 4. Zero-downtime deploy
- readiness 옵션이 없는 경우 배포 중 서비스 요청처리 실패함
![readiness_1](https://user-images.githubusercontent.com/26760226/106847381-2099c800-66f2-11eb-8753-35880af66203.png)
- customer의 deployment.yml 에 readiness 옵션 추가 <br>
![readiness_4](https://user-images.githubusercontent.com/26760226/106847498-5d65bf00-66f2-11eb-8480-99a636960c91.png)
- 기존 버전과 새 버전의 pod 공존 중
![readiness_2](https://user-images.githubusercontent.com/26760226/106847383-21325e80-66f2-11eb-9bc9-9d90d398f4b0.png)
- Availability 100% 확인 <br>
![readiness_3](https://user-images.githubusercontent.com/26760226/106847385-21caf500-66f2-11eb-8459-cbbe29b3a63f.png)

### 5. Config map
- order의 application.yaml <br>
![configmap_1](https://user-images.githubusercontent.com/26760226/106847760-e8df5000-66f2-11eb-8a05-30faf30a8e22.png)
- order의 deployment.yaml <br>
![configmap_2](https://user-images.githubusercontent.com/26760226/106847764-e977e680-66f2-11eb-998a-20911a5640c7.png)
- Config map 생성
``` bash
kubectl create configmap apiurl --from-literal=productapiurl=http://product:8080 --from-literal=stockapiurl=http://stock:8080 --from-literal=customerapiurl=http://customer:8080
```
![deploy_3](https://user-images.githubusercontent.com/26760226/106842174-d95a0a00-66e6-11eb-9b27-444dc1d36868.png)
![configmap_3](https://user-images.githubusercontent.com/26760226/106847766-ea107d00-66f2-11eb-8014-eb6ecee5dc83.png)

### 6. Self-healing (Liveness Probe)

