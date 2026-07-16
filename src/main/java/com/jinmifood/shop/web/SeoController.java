package com.jinmifood.shop.web;

import com.jinmifood.shop.config.properties.AppProperties;
import com.jinmifood.shop.repository.ProductRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeoController {
    private final ProductRepository products;
    private final String publicBaseUrl;

    public SeoController(ProductRepository products, AppProperties app) {
        this.products = products;
        this.publicBaseUrl = app.getPublicBaseUrl().toString().replaceAll("/$", "");
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    String sitemap() {
        var xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        append(xml, "/");
        append(xml, "/products");
        append(xml, "/about");
        append(xml, "/policies");
        products.findByActiveTrueOrderByCreatedAtDesc().forEach(product -> append(xml, "/products/" + product.getSlug()));
        return xml.append("</urlset>").toString();
    }

    private void append(StringBuilder xml, String path) {
        xml.append("<url><loc>").append(publicBaseUrl).append(path).append("</loc></url>");
    }
}
