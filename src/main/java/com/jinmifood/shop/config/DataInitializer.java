package com.jinmifood.shop.config;
import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import java.util.*;

@Configuration @Profile("!prod")
public class DataInitializer {
    @Bean CommandLineRunner seed(CategoryRepository categories,ProductRepository products,CustomerOrderRepository orders){return args->{
        orders.findAll().forEach(order->{order.repairLegacyPaymentStatus();orders.save(order);});
        if(categories.count()>0){
            repairCorruptedProductText(products);
            return;
        }
        var data=List.of(new Category("짝태","jjagtae",1),new Category("먹태","meoktae",2),new Category("장아찌","jangajji",3),new Category("미역","miyeok",4),new Category("오징어","squid",5),new Category("가공식품","processed",6));
        categories.saveAll(data);Map<String,Category> c=new HashMap<>();data.forEach(x->c.put(x.getSlug(),x));
        products.saveAll(List.of(
            p("먹태 39~41cm 1kg 내외","meoktae-1kg",35000,37000,24,"노릇하게 구워 고소한 먹태","술안주와 간식으로 좋은 큼직한 먹태입니다.","meoktae",true),
            p("북어채 1kg","dried-pollack-1kg",30000,32000,30,"국과 무침에 편리한 북어채","먹기 좋게 찢어 손질한 담백한 북어채입니다.","meoktae",true),
            p("쫄깃한 짝태 10미","jjagtae-10",39000,42000,20,"구워 먹기 좋은 담백한 짝태","먹기 좋게 손질해 술안주와 간식으로 활용하기 좋습니다.","jjagtae",true),
            p("반건조 오징어 10미","semi-dried-squid-10",42000,45000,18,"쫄깃하고 촉촉한 반건조 오징어","가정에서 간편하게 굽기 좋은 구성입니다.","squid",true),
            p("마른 오징어 5미","dried-squid-5",38000,40000,15,"씹을수록 진한 감칠맛","선별한 오징어를 알맞게 건조했습니다.","squid",false),
            p("완도 자른 미역 500g","wando-seaweed-500",15000,18000,40,"깨끗하게 손질한 완도산 미역","국과 냉국에 바로 사용하기 편합니다.","miyeok",true),
            p("무말랭이 장아찌 1kg","radish-pickle-1kg",14000,16000,22,"오독오독한 밥도둑 반찬","과하지 않은 양념으로 매일 먹기 좋습니다.","jangajji",true),
            p("건새우 500g","dried-shrimp-500",15000,20000,35,"볶음과 육수에 좋은 건새우","선별 후 깔끔하게 포장했습니다.","processed",false),
            p("고구마줄기 1kg","sweet-potato-stem-1kg",15000,25000,20,"나물 요리에 좋은 건고구마줄기","충분히 불려 각종 나물 요리에 활용하세요.","processed",false)
        ).stream().peek(x->x.setCategory(c.get(x.getCategory().getSlug()))).toList());
    };}

    private void repairCorruptedProductText(ProductRepository products){
        products.findAll().forEach(product->{
            if(!looksCorrupted(product.getName()) && !looksCorrupted(product.getSummary())
                    && !looksCorrupted(product.getDescription())) return;
            if("meoktae-1kg".equals(product.getSlug())){
                product.setName("먹태 39~41cm 1kg 내외");
                product.setSummary("노릇하게 구워 고소한 먹태");
                product.setDescription("술안주와 간식으로 좋은 큼직한 먹태입니다.");
                products.save(product);
            }else if("dried-pollack-1kg".equals(product.getSlug())){
                product.setName("북어채 1kg");
                product.setSummary("국과 무침에 편리한 북어채");
                product.setDescription("먹기 좋게 찢어 손질한 담백한 북어채입니다.");
                products.save(product);
            }else if("radish-pickle-1kg".equals(product.getSlug())){
                product.setName("무말랭이 장아찌 1kg");
                product.setSummary("오독오독한 밥도둑 반찬");
                product.setDescription("과하지 않은 양념으로 매일 먹기 좋습니다.");
                products.save(product);
            }
        });
    }

    private boolean looksCorrupted(String value){
        return value!=null && (value.indexOf('\u00eb')>=0 || value.indexOf('\u00ec')>=0
                || value.indexOf('\u00ea')>=0 || value.indexOf('\ufffd')>=0);
    }
    private Product p(String n,String s,int p,Integer o,int stock,String summary,String d,String category,boolean featured){
        var product=new Product(n,s,p,o,stock,summary,d,"/images/product-placeholder.svg",featured,new Category("temp",category,0));
        product.setOrigin("상품 포장지 별도 표기");product.setManufacturer("상품 포장지 별도 표기");
        product.setWeight("상품명 및 상세정보 참조");product.setShelfLife("상품 포장지 별도 표기");
        product.setStorageMethod("수령 후 냉장 또는 냉동 보관");return product;
    }
}
