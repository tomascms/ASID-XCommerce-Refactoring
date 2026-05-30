package ma.aui.sse.it.xcommerce.monolithic.controllers;

import ma.aui.sse.it.xcommerce.monolithic.data.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.test.context.support.WithMockUser;

import ma.aui.sse.it.xcommerce.monolithic.data.dtos.Product;
import ma.aui.sse.it.xcommerce.monolithic.data.dtos.ShoppingCart;
import ma.aui.sse.it.xcommerce.monolithic.services.ShoppingCartService;

/**
 *
 * @author Omar IRAQI
 */
@RestController
@RequestMapping("/rest/shoppingCart")
//@WithMockUser
public class ShoppingCartRestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @GetMapping("/get")
    public ShoppingCart getShoppingCart(Authentication auth){
        long userId = userRepository.findByUsername(auth.getName()).getId();
        System.out.println(shoppingCartService);
        return shoppingCartService.getShoppingCart(userId);
    }

    @PatchMapping("/addProduct")
    public ShoppingCart addProduct(Authentication auth, @RequestBody Product dto){
        long userId = userRepository.findByUsername(auth.getName()).getId();
        ShoppingCart shoppingCart = shoppingCartService.getShoppingCart(userId);
        return shoppingCartService.addProduct(shoppingCart, userId,
                                                dto.getId(),
                                                dto.getQuantity());
    }

    @PatchMapping("/decreaseProductQuantity")
    public ShoppingCart decreaseProductQuantity(Authentication auth, @RequestBody Product dto){
        long userId = userRepository.findByUsername(auth.getName()).getId();
        ShoppingCart shoppingCart = shoppingCartService.getShoppingCart(userId);
        return shoppingCartService.decreaseProductQuantity(shoppingCart, userId, dto.getId(),
                                                    dto.getQuantity());
    }

    @PatchMapping("/removeProduct")
    public ShoppingCart removeProduct(Authentication auth, @RequestBody Product dto){
        long userId = userRepository.findByUsername(auth.getName()).getId();
        ShoppingCart shoppingCart = shoppingCartService.getShoppingCart(userId);
        return shoppingCartService.removeProduct(shoppingCart, userId, dto.getId());
    }
}