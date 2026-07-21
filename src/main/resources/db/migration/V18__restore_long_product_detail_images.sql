UPDATE products
SET detail_image_url = REPLACE(detail_image_url, '-ai-intro.jpg', '-long-detail.jpg'),
    updated_at = CURRENT_TIMESTAMP
WHERE detail_image_url LIKE '%-ai-intro.jpg';
