UPDATE products AS product
SET image_url = '/images/catalog/' || assets.asset_id || '-ai-main.jpg',
    detail_image_url = '/images/catalog/' || assets.asset_id || '-ai-detail.jpg',
    updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('dried-pollack-1kg', '1746168600'),
    ('egg-jjagtae-500g', '1746168499'),
    ('jjagtae-assorted-size', '1746168229'),
    ('peeled-jjagtae-strips-500g', '1746168000'),
    ('meoktae-1kg', '1746168960'),
    ('gochujang-pickles-500g', '1746169070'),
    ('dried-radish-1kg', '1746168838'),
    ('vegetable-pickles-500g', '1746167513'),
    ('seaweed-bundle-5kg', '1746168728'),
    ('seaweed-stem-5kg', '1746167889'),
    ('ssam-seaweed-5kg', '1746167773'),
    ('young-seaweed-stem-5kg', '1746166839'),
    ('dried-squid-10', '1746168372'),
    ('semi-dried-baby-squid', '1746167375'),
    ('squid-strips-1kg', '1746166486'),
    ('frozen-squid-mouth-2kg', '1746166104'),
    ('dried-shrimp-500', '1746169275'),
    ('sweet-potato-stem-1kg', '1746169174'),
    ('dried-green-chili-1kg', '1746168110'),
    ('suanlafen-12', '1746167666'),
    ('spicy-crawfish-2100g', '1746167191'),
    ('spring-roll-wrappers-5', '1746166995')
) AS assets(slug, asset_id)
WHERE product.slug = assets.slug;
