UPDATE products
SET detail_image_url = REPLACE(detail_image_url, '-long-detail.jpg', '-crafted-detail.jpg'),
    updated_at = CURRENT_TIMESTAMP
WHERE detail_image_url LIKE '%-long-detail.jpg';
