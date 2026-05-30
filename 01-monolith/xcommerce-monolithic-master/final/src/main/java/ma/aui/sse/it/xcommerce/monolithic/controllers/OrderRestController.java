package ma.aui.sse.it.xcommerce.monolithic.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ma.aui.sse.it.xcommerce.monolithic.data.entities.Order;
import ma.aui.sse.it.xcommerce.monolithic.data.entities.OrderStatus;
import ma.aui.sse.it.xcommerce.monolithic.data.entities.User;
import ma.aui.sse.it.xcommerce.monolithic.data.repositories.UserRepository;
import ma.aui.sse.it.xcommerce.monolithic.services.OrderService;

/**
 *
 * @author Omar IRAQI
 */
@RestController
@RequestMapping("/rest/order")
public class OrderRestController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;


    @GetMapping("/list")
    public List<Order> getOrdersByCustomer(Authentication auth) {
        long customerId = resolveCustomerId(auth);
        return orderService.getOrdersByCustomer(customerId);
    }

    @GetMapping("/checkout")
    public void checkout(Authentication auth) {
        long customerId = resolveCustomerId(auth);
        orderService.checkout(customerId);
    }

    @GetMapping("/backOffice/list")
    public List<Order> getOrdersByCustomer(@RequestParam long customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }

    @GetMapping("/backOffice/updateStatus")
    public void updateOrderStatus(@RequestParam long orderId, @RequestParam int newStatus){
        switch(newStatus){
            case 1:
                orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED);
                break;
            case 2:
                orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);
                break;
            case 3:
                orderService.updateOrderStatus(orderId, OrderStatus.ONHOLD);
                break;
            case 4:
                orderService.updateOrderStatus(orderId, OrderStatus.CANCELED);
                break;
        }
    }

    /**
     * Resolve o ID do utilizador autenticado a partir do {@code Authentication}
     * injetado pelo Spring Security. Substitui o antigo {@code userId = 1}
     * hardcoded para suportar multi-utilizador real via JWT.
     */
    private long resolveCustomerId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Utilizador nao autenticado.");
        }
        User user = userRepository.findByUsername(auth.getName());
        if (user == null) {
            throw new RuntimeException("Utilizador nao encontrado: " + auth.getName());
        }
        return user.getId();
    }
}
