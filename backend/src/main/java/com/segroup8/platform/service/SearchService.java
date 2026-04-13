package com.segroup8.platform.service;

import com.segroup8.platform.dto.ProductPageQueryRequest;
import com.segroup8.platform.dto.SecondhandProductPageQueryRequest;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.search.FuseAdapter;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductVO;
import com.segroup8.platform.vo.SecondhandProductVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一搜索服务：聚合新品与二手商品，并通过 FuseAdapter 执行模糊搜索。
 */
@Service
public class SearchService {

    private static final long PAGE_SIZE = 200L;

    private final ProductService productService;
    private final SecondhandProductService secondhandProductService;
    private final FuseAdapter fuseAdapter;

    public SearchService(ProductService productService,
            SecondhandProductService secondhandProductService,
            FuseAdapter fuseAdapter) {
        this.productService = productService;
        this.secondhandProductService = secondhandProductService;
        this.fuseAdapter = fuseAdapter;
    }

    public List<Product> search(String keyword) {
        return search(keyword, 0.3D);
    }

    public List<Product> search(String keyword, Double threshold) {
        List<Product> allProducts = new ArrayList<>();
        allProducts.addAll(fetchAllProducts());
        allProducts.addAll(fetchAllSecondhandAsProducts());
        return fuseAdapter.fuzzySearchProducts(allProducts, keyword, threshold);
    }

    private List<Product> fetchAllProducts() {
        List<ProductVO> records = new ArrayList<>();
        long pageNum = 1L;

        while (true) {
            ProductPageQueryRequest request = new ProductPageQueryRequest();
            request.setPageNum(pageNum);
            request.setPageSize(PAGE_SIZE);
            request.setKeyword(null);

            PageVO<ProductVO> page = productService.pagePublicProducts(request);
            List<ProductVO> current = page.getRecords() == null ? List.of() : page.getRecords();
            records.addAll(current);

            if (records.size() >= page.getTotal() || current.isEmpty()) {
                break;
            }
            pageNum++;
        }

        return records.stream().map(this::toProduct).toList();
    }

    private List<Product> fetchAllSecondhandAsProducts() {
        List<SecondhandProductVO> records = new ArrayList<>();
        long pageNum = 1L;

        while (true) {
            SecondhandProductPageQueryRequest request = new SecondhandProductPageQueryRequest();
            request.setPageNum(pageNum);
            request.setPageSize(PAGE_SIZE);
            request.setKeyword(null);

            PageVO<SecondhandProductVO> page = secondhandProductService.pagePublicProducts(request);
            List<SecondhandProductVO> current = page.getRecords() == null ? List.of() : page.getRecords();
            records.addAll(current);

            if (records.size() >= page.getTotal() || current.isEmpty()) {
                break;
            }
            pageNum++;
        }

        return records.stream().map(this::toProduct).toList();
    }

    private Product toProduct(ProductVO vo) {
        Product product = new Product();
        product.setId(vo.getId());
        product.setShopId(vo.getShopId());
        product.setName(vo.getName());
        product.setCover(vo.getCover());
        product.setDescription(vo.getDescription());
        product.setPrice(vo.getPrice());
        product.setStock(vo.getStock());
        product.setStatus(vo.getStatus());
        product.setCreateTime(vo.getCreateTime());
        return product;
    }

    private Product toProduct(SecondhandProductVO vo) {
        Product product = new Product();
        product.setId(vo.getId());
        product.setShopId(vo.getSellerUserId());
        product.setName(vo.getName());
        product.setCover(vo.getCover());
        product.setDescription(vo.getDescription());
        product.setPrice(vo.getSalePrice());
        product.setStock(1);
        product.setStatus(vo.getStatus());
        product.setCreateTime(vo.getCreateTime());
        return product;
    }
}
