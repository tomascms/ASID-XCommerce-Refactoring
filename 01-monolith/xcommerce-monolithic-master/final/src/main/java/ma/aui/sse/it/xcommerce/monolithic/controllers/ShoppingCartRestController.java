package ma.aui.sse.it.xcommerce.monolithic.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ma.aui.sse.it.xcommerce.monolithic.data.dtos.Product;
import ma.aui.sse.it.xcommerce.monolithic.data.dtos.ShoppingCart;
import ma.aui.sse.it.xcommerce.monolithic.data.entities.User;
import ma.aui.sse.it.xcommerce.monolithic.data.repositories.UserRepository;
import ma.aui.sse.it.xcommerce.monolithic.services.ShoppingCartService;

/**
 *
 * @author Omar IRAQI
 */
@RestController
@RequestMapping("/rest/shoppingCart")
public class ShoppingCartRestController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/get")
    public ShoppingCart getShoppingCart(Authentication auth) {
        long userId = resolveUserId(auth);
        return shoppingCartService.getShoppingCart(userId);
    }

    @PatchMapping("/addProduct")
    public ShoppingCart addProduct(Authentication auth, @RequestBody Product dto) {
        long userId = resolveUserId(auth);
        ShoppingCart shoppingCart = shoppingCartService.getShoppingCart(userId);
        return shoppingCartService.addProduct(shoppingCart, userId,
                                                dto.getId(),
                                                dto.getQuantity());
    }

    @PatchMapping("/decreaseProductQuantity")
    public ShoppingCart decreaseProductQuantity(Authentication auth, @RequestBody Product dto) {
        long userId = resolveUserId(auth);
        ShoppingCart shoppingCart = shoppingCartService.getShoppingCart(userId);
        return shoppingCartService.decreaseProductQuantity(shoppingCart, userId, dto.getId(),
                                                    dto.getQuantity());
    }

    @PatchMapping("/removeProduct")
    public ShoppingCart removeProduct(Authentication auth, @RequestBody Product dto) {
        long userId = resolveUserId(auth);
        ShoppingCart shoppingCart = shoppingCartService.getShoppingCart(userId);
        return shoppingCartService.removeProduct(shoppingCart, userId, dto.getId());
    }

    /**
     * Resolve o ID do utilizador autenticado a partir do {@code Authentication}
     * injetado pelo Spring Security. Substitui o antigo {@code userId = 1}
     * hardcoded para suportar multi-utilizador real via JWT.
     */
    private long resolveUserId(Authentication auth) {
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
