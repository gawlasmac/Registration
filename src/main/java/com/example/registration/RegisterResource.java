package com.example.registration;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.SynchronousQueue;

@RestController("/")
@RequiredArgsConstructor
public class RegisterResource {

    private final RestTemplate restTemplate;
    private final Environment env;

    private Queue<CloseCustomer> closeRequests = new SynchronousQueue<>();
    private Queue<RegisterCustomer> registerRequests = new SynchronousQueue<>();

    @PostMapping("/register")
    @HystrixCommand(fallbackMethod = "fallbackRegisterCustomer", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500")
    })
    public ResponseEntity<HttpStatus> registerCustomer(@RequestBody final RegisterCustomer customer) {
        ResponseEntity<RegisterCustomer[]> customers = restTemplate.getForEntity("http://Customers/customers", RegisterCustomer[].class);
        Optional<RegisterCustomer> foundCustomer = Arrays.asList(customers.getBody()).stream().filter(cust -> cust.getFirstName().equals(customer.getFirstName()) && cust.getLastName().equals(customer.getLastName())).findFirst();
        if (foundCustomer.isPresent()) {
            return new ResponseEntity(HttpStatus.CONFLICT);
        }
        Customer newCustomer = new Customer(customer.getFirstName(), customer.getLastName(), false, new ArrayList<>());
        restTemplate.postForObject("http://Customers/customers", newCustomer, ResponseEntity.class);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<HttpStatus> fallbackRegisterCustomer(@RequestBody final RegisterCustomer customer) {
        registerRequests.add(customer);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/active")
    public ResponseEntity<HttpStatus> activateCustomer(@RequestBody final ActivateCustomer customer) {
        ResponseEntity<ActivateCustomer[]> customers = restTemplate.getForEntity("http://Customers/customers?firstName=" + customer.getFirstName() + "&lastName=" + customer.getLastName(), ActivateCustomer[].class);
        if (Objects.requireNonNull(customers.getBody()).length == 0) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        ActivateCustomer foundCustomer = customers.getBody()[0];
        if (foundCustomer.isActive()) {
            return new ResponseEntity(HttpStatus.CONFLICT);
        }
        customer.setActive(true);
        restTemplate.postForObject("http://Customers/updateCustomer", customer, ResponseEntity.class);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/close")
    @HystrixCommand(fallbackMethod = "fallbackCloseCustomer", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500")
    })
    public ResponseEntity<HttpStatus> closeCustomer(@RequestBody final CloseCustomer customer) {
        ResponseEntity<CloseCustomer[]> customers = restTemplate.getForEntity("http://Customers/customers?firstName=" + customer.getFirstName() + "&lastName=" + customer.getLastName(), CloseCustomer[].class);
        if (Objects.requireNonNull(customers.getBody()).length == 0) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        CloseCustomer foundCustomer = customers.getBody()[0];
        restTemplate.delete("http://Customers/customers/" + foundCustomer.getId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<HttpStatus> fallbackCloseCustomer(@RequestBody final CloseCustomer customer) {
        closeRequests.add(customer);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Scheduled(fixedRate = 10000)
    void scheduledRegistration() {
        if (registerRequests.size() > 0) {
            RegisterCustomer request = registerRequests.peek();
            if (registerCustomer(request).getStatusCode() == HttpStatus.OK) {
                registerRequests.poll();
            }
        }
        if (closeRequests.size() > 0) {
            CloseCustomer request = closeRequests.peek();
            if (closeCustomer(request).getStatusCode() == HttpStatus.OK) {
                closeRequests.poll();
            }
        }
    }

    @GetMapping("/version")
    public String getVersion() {
        return env.getProperty("custom.api.version");
    }
}
