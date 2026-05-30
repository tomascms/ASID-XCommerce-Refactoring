package ma.aui.sse.it.xcommerce.monolithic.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ma.aui.sse.it.xcommerce.monolithic.data.entities.User;
import ma.aui.sse.it.xcommerce.monolithic.data.entities.Order;
import ma.aui.sse.it.xcommerce.monolithic.data.entities.OrderStatus;
import ma.aui.sse.it.xcommerce.monolithic.data.entities.Product;
import ma.aui.sse.it.xcommerce.monolithic.data.dtos.ShoppingCart;
import ma.aui.sse.it.xcommerce.monolithic.data.repositories.UserRepository;
import ma.aui.sse.it.xcommerce.monolithic.data.repositories.OrderRepository;
import ma.aui.sse.it.xcommerce.monolithic.data.repositories.ProductRepository;

/**
 *
 * @author Omar IRAQI
 */
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShoppingCartService shoppingCartService;

    public List<Order> getOrdersByCustomer(long customerId) {
        return orderRepository.findByCustomer(customerId);
    }

    public void checkout(long userId) {
        User user = userRepository.findById(userId).get();
        ShoppingCart shoppingCart = shoppingCartService.getShoppingCart(userId);
        if(shoppingCart == null || shoppingCart.getProductsTotalPrice() == 0)
            return;

        // Validacao de stock antes de criar a order — paridade funcional
        // com o inventory-service dos microservicos. Apenas valida; nao decrementa.
        for (Map.Entry<Product, Integer> entry : shoppingCart.getSelectedProducts().entrySet()) {
            Product cartProduct = entry.getKey();
            int requestedQty = entry.getValue();
            Product current = productRepository.findById(cartProduct.getId()).orElseThrow(
                () -> new RuntimeException("Produto nao encontrado: " + cartProduct.getId()));
            if (current.getQuantity() < requestedQty) {
                throw new RuntimeException(
                    "Stock insuficiente para o produto " + current.getId()
                    + " (disponivel: " + current.getQuantity()
                    + ", pedido: " + requestedQty + ")");
            }
        }

        Order order = new Order(shoppingCart, user);
        orderRepository.save(order);
        shoppingCartService.empty(shoppingCart, user.getId());
    }

    public void updateOrderStatus(long orderId, OrderStatus newStatus){
        Order order = orderRepository.findById(orderId).get();
        order.updateStatus(newStatus);
        orderRepository.save(order);
    }
}