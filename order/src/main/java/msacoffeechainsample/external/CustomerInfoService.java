
package msacoffeechainsample.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="customer", url="${api.customer.url}")
public interface CustomerInfoService {

    @RequestMapping(method= RequestMethod.POST, path="/customers/{customerId}/level")
    public String checkLevel(@PathVariable("customerId") Long customerId);

}