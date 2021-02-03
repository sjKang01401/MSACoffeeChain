package msacoffeechainsample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class CustomerInfoController {

	@Autowired
	CustomerInfoRepository customerInfoRepository;

	@RequestMapping(method= RequestMethod.POST, path="/customers/{customerId}/level")
	public String checkLevel(@PathVariable("customerId") Long id) {

		Optional<CustomerInfo> customerOptional = customerInfoRepository.findById(id);

		if (customerOptional.isPresent()) {

			CustomerInfo customerInfo = customerOptional.get();
			String customerLevel = "";

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

			customerInfo.setLevel(customerLevel);

			customerInfoRepository.save(customerInfo);

			return customerLevel;
		}

		return "Iron";
	}

	@RequestMapping(method=RequestMethod.GET, path="/customers")
	public Iterable<CustomerInfo> getAll() {
		return customerInfoRepository.findAll();
	}

	@RequestMapping(method=RequestMethod.GET, path="/customers/{id}")
	public Optional<CustomerInfo> getOne(@PathVariable("id") Long id) {
		return customerInfoRepository.findById(id);
	}

	@RequestMapping(method=RequestMethod.POST, path="/customers")
	public CustomerInfo post(@RequestBody CustomerInfo stock) {
		return customerInfoRepository.save(stock);
	}

	@RequestMapping(method=RequestMethod.PATCH, path="/customers/{id}")
	public CustomerInfo patch(@PathVariable("id") Long id, @RequestBody CustomerInfo inputCustomerInfo) {

		Optional<CustomerInfo> customerOptional = customerInfoRepository.findById(id);

		if(!customerOptional.isPresent()) return null;

		CustomerInfo customerInfo = customerOptional.get();
		if (inputCustomerInfo.getName() != null) customerInfo.setName(inputCustomerInfo.getName());
		if (inputCustomerInfo.getLevel() != null) customerInfo.setLevel(inputCustomerInfo.getLevel());
		if (inputCustomerInfo.getScore() != null) customerInfo.setScore(inputCustomerInfo.getScore());

		return customerInfoRepository.save(customerInfo);
	}

	@RequestMapping(method=RequestMethod.DELETE, path="/customers/{id}")
	public void delete(@PathVariable("id") Long id) {
		Optional<CustomerInfo> customerOptional = customerInfoRepository.findById(id);

		if(!customerOptional.isPresent()) return;

		customerInfoRepository.delete(customerOptional.get());
	}
}
