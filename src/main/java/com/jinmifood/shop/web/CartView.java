package com.jinmifood.shop.web;
import com.jinmifood.shop.domain.Product;
import java.util.List;
public record CartView(List<Line> lines,long subtotal,long shippingFee,long total){ public record Line(Product product,int quantity,long lineTotal){} }
