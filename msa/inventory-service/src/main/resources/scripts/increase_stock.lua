-- 재고 증가 Lua 스크립트
-- KEYS[1]: 재고 키 (stock:{productId})
-- ARGV[1]: 증가량
-- ARGV[2]: TTL (초)
-- 반환: 증가 후 재고

local key = KEYS[1]
local amount = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

-- 증가 및 TTL 설정 (atomic)
local newStock = redis.call('INCRBY', key, amount)
redis.call('EXPIRE', key, ttl)

return newStock
