ALTER TABLE products ADD COLUMN detail_image_url VARCHAR(500);

INSERT INTO products
    (name, slug, price, original_price, stock, summary, description, image_url, detail_image_url,
     origin, manufacturer, weight, shelf_life, storage_method, active, featured,
     category_id, created_at, updated_at)
SELECT name, slug, price, original_price, 50, summary, description,
       '/images/catalog/' || source_id || '-main.jpg',
       '/images/catalog/' || source_id || '-detail.jpg',
       '중국/진미식품', '진미푸드', weight, '상품 포장지 별도 표기', storage_method,
       TRUE, featured, (SELECT id FROM categories WHERE categories.slug = category_slug),
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('1746168600', '진미식품 북어채 1kg', 'dried-pollack-1kg', 30000, 32000, '북어채 1kg', '국과 무침, 반찬에 활용하기 좋은 북어채입니다.', '1kg', '직사광선을 피하고 서늘한 곳에 보관', TRUE, 'meoktae'),
    ('1746168499', '진미식품 알짝태 500g', 'egg-jjagtae-500g', 22000, 25000, '알짝태 500g', '담백하고 쫄깃한 알짝태 500g 상품입니다.', '500g', '직사광선을 피하고 서늘한 곳에 보관', TRUE, 'jjagtae'),
    ('1746168229', '진미식품 짝태 42cm 1.1kg 부터 42~50cm 종류별', 'jjagtae-assorted-size', 35000, 40000, '짝태 42cm 1.1kg 부터 42~50cm 종류별', '크기별로 선택할 수 있는 진미식품 짝태입니다.', '1.1kg부터, 크기별 상이', '직사광선을 피하고 서늘한 곳에 보관', TRUE, 'jjagtae'),
    ('1746168000', '진미식품 탈피짝태채 500g', 'peeled-jjagtae-strips-500g', 23000, 25000, '탈피짝태채 500g', '껍질을 벗겨 간편하게 즐길 수 있는 짝태채입니다.', '500g', '직사광선을 피하고 서늘한 곳에 보관', TRUE, 'jjagtae'),
    ('1746168960', '진미식품 먹태 39~41cm 1kg 내외', 'meoktae-1kg', 35000, 37000, '먹태 39~41cm 1kg 내외', '노릇하게 구워 안주와 간식으로 즐기기 좋은 먹태입니다.', '1kg 내외', '직사광선을 피하고 서늘한 곳에 보관', TRUE, 'meoktae'),
    ('1746169070', '진미식품 고추장장아찌 모듬장아찌 500g', 'gochujang-pickles-500g', 13000, 15000, '고추장장아찌 모듬장아찌 500g', '매콤한 고추장 양념의 모듬 장아찌입니다.', '500g', '수령 후 냉장 보관', TRUE, 'jangajji'),
    ('1746168838', '진미식품 무말랭이 1kg', 'dried-radish-1kg', 12000, 13000, '무말랭이 1kg', '오독오독한 식감의 무말랭이 1kg 상품입니다.', '1kg', '직사광선을 피하고 서늘한 곳에 보관', FALSE, 'jangajji'),
    ('1746167513', '진미식품 모듬장아찌 야채장아찌 500g', 'vegetable-pickles-500g', 13000, 15000, '모듬장아찌 야채장아찌 500g', '다양한 야채를 한 번에 즐기는 모듬 장아찌입니다.', '500g', '수령 후 냉장 보관', FALSE, 'jangajji'),
    ('1746168728', '진미식품 미역묶음 미역줄기 쌈미역 미역묘목 5kg', 'seaweed-bundle-5kg', 60000, 65000, '미역묶음 미역줄기 쌈미역 미역묘목 5kg', '미역줄기와 쌈미역 등을 묶어 구성한 5kg 상품입니다.', '5kg', '수령 후 냉장 보관', TRUE, 'miyeok'),
    ('1746167889', '진미식품 미역줄기 5kg', 'seaweed-stem-5kg', 48000, 50000, '미역줄기 5kg', '반찬과 무침에 활용하기 좋은 미역줄기입니다.', '5kg', '수령 후 냉장 보관', FALSE, 'miyeok'),
    ('1746167773', '진미식품 쌈미역 5kg', 'ssam-seaweed-5kg', 48000, 50000, '쌈미역 5kg', '쌈과 무침으로 즐길 수 있는 쌈미역입니다.', '5kg', '수령 후 냉장 보관', FALSE, 'miyeok'),
    ('1746166839', '진미식품 새싹 미역줄기 5kg', 'young-seaweed-stem-5kg', 60000, 65000, '새싹 미역줄기 5kg', '부드러운 식감의 새싹 미역줄기입니다.', '5kg', '수령 후 냉장 보관', FALSE, 'miyeok'),
    ('1746168372', '진미식품 오징어 10마리 소/대', 'dried-squid-10', 60000, 65000, '오징어 10마리 소/대', '크기를 선택할 수 있는 오징어 10마리 구성입니다.', '10마리, 크기별 상이', '직사광선을 피하고 서늘한 곳에 보관', TRUE, 'squid'),
    ('1746167375', '진미식품 반건조꼴뚜기 500g 3봉지 12봉지', 'semi-dried-baby-squid', 45000, 50000, '반건조꼴뚜기 500g 3봉지 12봉지', '쫄깃한 반건조 꼴뚜기를 묶음으로 구성했습니다.', '500g 단위, 구성별 상이', '수령 후 냉동 보관', FALSE, 'squid'),
    ('1746166486', '진미식품 오징어채 오징어실채 500g 2봉지', 'squid-strips-1kg', 54000, 56000, '오징어채 오징어실채 500g 2봉지', '반찬과 간식으로 활용하기 좋은 오징어실채입니다.', '500g × 2봉지', '직사광선을 피하고 서늘한 곳에 보관', FALSE, 'squid'),
    ('1746166104', '진미식품 냉동 오징어입 입빨제거 1kg 2봉지', 'frozen-squid-mouth-2kg', 32000, 40000, '냉동 오징어입 입빨제거 1kg 2봉지', '입빨을 제거해 손질 부담을 줄인 냉동 오징어입입니다.', '1kg × 2봉지', '수령 즉시 냉동 보관', FALSE, 'squid'),
    ('1746169275', '진미식품 건새우 500g 1봉지 2봉지', 'dried-shrimp-500', 15000, 20000, '건새우 500g 1봉지 2봉지', '볶음과 육수에 활용하기 좋은 건새우입니다.', '500g 단위, 구성별 상이', '직사광선을 피하고 서늘한 곳에 보관', TRUE, 'processed'),
    ('1746169174', '진미식품 고구마줄기 1kg', 'sweet-potato-stem-1kg', 15000, 25000, '고구마줄기 1kg', '나물 요리에 활용하기 좋은 고구마줄기입니다.', '1kg', '직사광선을 피하고 서늘한 곳에 보관', FALSE, 'processed'),
    ('1746168110', '진미식품 청건고추 1kg', 'dried-green-chili-1kg', 32000, 35000, '청건고추 1kg', '요리에 매콤한 풍미를 더하는 청건고추입니다.', '1kg', '직사광선을 피하고 서늘한 곳에 보관', FALSE, 'processed'),
    ('1746167666', '진미식품 쏸라펀 1박스(12팩)', 'suanlafen-12', 33600, 35000, '쏸라펀 1박스(12팩)', '새콤하고 매콤한 쏸라펀 12팩 박스 구성입니다.', '12팩', '직사광선을 피하고 서늘한 곳에 보관', FALSE, 'processed'),
    ('1746167191', '진미식품 라이커가재 700g 3팩 묶음', 'spicy-crawfish-2100g', 45000, 50000, '라이커가재 700g 3팩 묶음', '간편하게 즐기는 라이커가재 3팩 묶음입니다.', '700g × 3팩', '수령 즉시 냉동 보관', FALSE, 'processed'),
    ('1746166995', '진미식품 전병 320g 춘병 350g 5봉지 혼합', 'spring-roll-wrappers-5', 20000, 22000, '전병 320g 춘병 350g 5봉지 혼합', '전병과 춘병을 골라 구성하는 5봉지 혼합 상품입니다.', '5봉지, 구성별 상이', '수령 후 냉동 보관', FALSE, 'processed')
) AS source(source_id, name, slug, price, original_price, summary, description, weight, storage_method, featured, category_slug)
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    original_price = EXCLUDED.original_price,
    summary = EXCLUDED.summary,
    description = EXCLUDED.description,
    image_url = EXCLUDED.image_url,
    detail_image_url = EXCLUDED.detail_image_url,
    origin = EXCLUDED.origin,
    manufacturer = EXCLUDED.manufacturer,
    weight = EXCLUDED.weight,
    shelf_life = EXCLUDED.shelf_life,
    storage_method = EXCLUDED.storage_method,
    active = TRUE,
    featured = EXCLUDED.featured,
    category_id = EXCLUDED.category_id,
    updated_at = CURRENT_TIMESTAMP;

UPDATE products
SET active = FALSE, featured = FALSE, updated_at = CURRENT_TIMESTAMP
WHERE slug NOT IN (
    'dried-pollack-1kg', 'egg-jjagtae-500g', 'jjagtae-assorted-size',
    'peeled-jjagtae-strips-500g', 'meoktae-1kg', 'gochujang-pickles-500g',
    'dried-radish-1kg', 'vegetable-pickles-500g', 'seaweed-bundle-5kg',
    'seaweed-stem-5kg', 'ssam-seaweed-5kg', 'young-seaweed-stem-5kg',
    'dried-squid-10', 'semi-dried-baby-squid', 'squid-strips-1kg',
    'frozen-squid-mouth-2kg', 'dried-shrimp-500', 'sweet-potato-stem-1kg',
    'dried-green-chili-1kg', 'suanlafen-12', 'spicy-crawfish-2100g',
    'spring-roll-wrappers-5'
);
