package pl.shop.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shop.cart.client.InventoryServiceClient;
import pl.shop.cart.client.ProductServiceClient;
import pl.shop.cart.dto.request.AddCartItemRequest;
import pl.shop.cart.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.cart.dto.request.QuoteRequest;
import pl.shop.cart.dto.response.CartResponse;
import pl.shop.cart.dto.response.QuoteResponse;
import pl.shop.cart.dto.response.StockResponse;
import pl.shop.cart.exception.CartNotFoundException;
import pl.shop.cart.exception.InsufficientStockException;
import pl.shop.cart.mapper.CartMapper;
import pl.shop.cart.mapper.PriceMapper;
import pl.shop.cart.model.Cart;
import pl.shop.cart.model.CartItem;
import pl.shop.cart.repository.CartRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final PriceMapper priceMapper;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;

    @Transactional
    public CartResponse createCart() {
        log.debug("Attempting to create cart");
        Cart saved = cartRepository.save(Cart.create());
        log.info("Cart created with id = {}", saved.getId());
        return cartMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long cartId) {
        log.debug("Attempting to fetch cart with id = {}", cartId);
        Cart cart = fetchCart(cartId);
        log.info("Cart with id = {} fetched", cartId);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long cartId, AddCartItemRequest request) {
        log.debug("Attempting to add product with id = {} x{} to cart with id = {}", request.productId(), request.quantity(), cartId);
        Cart cart = fetchActiveCart(cartId);
        QuoteResponse quote = productServiceClient.quote(request.productId(), new QuoteRequest(request.configurationIds()));
        Map<Long, Integer> required = requiredQuantities(cart);
        addRequirement(required, quote.stockProductIds(), request.quantity());
        validateStock(required);
        CartItem item = cart.addItem(
                quote.productId(),
                quote.productName(),
                priceMapper.toEntity(quote.unitPrice()),
                request.quantity(),
                cartMapper.toComponents(quote.selectedComponents())
        );
        cartRepository.flush();
        log.info("Product with id = {} added to cart with id = {} as item with id = {}", quote.productId(), cartId, item.getId());
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse changeItemQuantity(Long cartId, Long itemId, ChangeCartItemQuantityRequest request) {
        log.debug("Attempting to change quantity of item with id = {} in cart with id = {} to {}", itemId, cartId, request.quantity());
        Cart cart = fetchActiveCart(cartId);
        CartItem item = cart.findItem(itemId);
        if (request.quantity() > item.getQuantity()) {
            Map<Long, Integer> required = requiredQuantities(cart);
            addRequirement(required, item.getStockProductIds(), request.quantity() - item.getQuantity());
            validateStock(required);
        }
        cart.changeItemQuantity(itemId, request.quantity());
        log.info("Quantity of item with id = {} in cart with id = {} changed to {}", itemId, cartId, request.quantity());
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long cartId, Long itemId) {
        log.debug("Attempting to remove item with id = {} from cart with id = {}", itemId, cartId);
        Cart cart = fetchActiveCart(cartId);
        cart.removeItem(itemId);
        cartRepository.flush();
        log.info("Item with id = {} removed from cart with id = {}", itemId, cartId);
        return cartMapper.toResponse(cart);
    }

    private Map<Long, Integer> requiredQuantities(Cart cart) {
        Map<Long, Integer> required = new LinkedHashMap<>();
        for (CartItem item : cart.getItems()) {
            addRequirement(required, item.getStockProductIds(), item.getQuantity());
        }
        return required;
    }

    private void addRequirement(Map<Long, Integer> required, List<Long> productIds, int quantity) {
        for (Long productId : productIds) {
            required.merge(productId, quantity, Integer::sum);
        }
    }

    private void validateStock(Map<Long, Integer> required) {
        Map<Long, Integer> available = fetchAvailableQuantities(List.copyOf(required.keySet()));
        required.forEach((productId, requiredQuantity) -> {
            int availableQuantity = available.getOrDefault(productId, 0);
            if (availableQuantity < requiredQuantity) {
                throw new InsufficientStockException(productId, requiredQuantity, availableQuantity);
            }
        });
    }

    private Map<Long, Integer> fetchAvailableQuantities(List<Long> productIds) {
        return inventoryServiceClient.getStock(productIds)
                .stream()
                .collect(Collectors.toMap(StockResponse::productId, StockResponse::quantity, Integer::sum));
    }

    private Cart fetchActiveCart(Long cartId) {
        Cart cart = fetchCart(cartId);
        cart.validateActive();
        return cart;
    }

    private Cart fetchCart(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
    }
}
