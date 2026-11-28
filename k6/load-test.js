import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,          // 20 người dùng đồng thời
  duration: '5m',   // chạy liên tục trong 5 phút
  thresholds: {
    http_req_failed: ['rate<0.01'],      // Tỉ lệ lỗi < 1%
    http_req_duration: ['p(95)<500'],    // 95% request phản hồi < 500ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export function setup() {
  const res = http.get(`${BASE_URL}/api/products?page=0&size=8&sort=NEWEST`);
  let productId = null;

  if (res.status === 200) {
    try {
      const body = res.json();
      if (body && Array.isArray(body.content) && body.content.length > 0) {
        productId = body.content[0].id;
      }
    } catch (_) {
      productId = null;
    }
  }

  return { productId };
}

export default function (data) {
  // 1. Lấy danh sách danh mục
  const resCategories = http.get(`${BASE_URL}/api/categories`);
  check(resCategories, {
    'categories status 200': (r) => r.status === 200,
  });

  // 2. Lấy danh sách thương hiệu
  const resBrands = http.get(`${BASE_URL}/api/brands`);
  check(resBrands, {
    'brands status 200': (r) => r.status === 200,
  });

  // 3. Lấy danh sách sản phẩm trang chủ
  const resProducts = http.get(`${BASE_URL}/api/products?page=0&size=8&sort=NEWEST`);
  check(resProducts, {
    'products status 200': (r) => r.status === 200,
  });

  // 4. Gợi ý tìm kiếm sản phẩm
  const resSuggest = http.get(`${BASE_URL}/api/products/suggest?q=a`);
  check(resSuggest, {
    'suggest status 200': (r) => r.status === 200,
  });

  // 5. Xem chi tiết sản phẩm (nếu có productId từ setup)
  if (data && data.productId) {
    const resDetails = http.get(`${BASE_URL}/api/products/${data.productId}`);
    check(resDetails, {
      'product details status 200': (r) => r.status === 200,
    });
  }

  sleep(1);
}
