import http from 'k6/http';

export const options = {
  scenarios: {
    purchase_requests: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1m',
      duration: '1m',
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
};

export default function () {
  http.post(
    'http://host.docker.internal:8080/api/v1/flash-sales/purchase',
    JSON.stringify({
      itemId: '44444444-0000-0000-0000-000000000002',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization:
          'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4ZDc3ZmE2NS04MWNiLTRiNWQtYjhiZi03MjlhZTlkODI3MzYiLCJyb2xlIjoiQlVZRVIiLCJwZXJtaXNzaW9ucyI6W10sImV4cCI6MTc4ODMzOTU0NSwidG9rZW5fdHlwZSI6ImFjY2VzcyIsImlhdCI6MTc4ODI1MzE0NSwianRpIjoiOWViZTY0N2YtMTIzZS00MjhiLTkwNDYtNzg1N2ZiZmEzNzRlIn0.Y73Z_jw3_R56uW20Zj3jUBNPvQNQePpSZYtb76P6y6s',
      },
    },
  );
}
