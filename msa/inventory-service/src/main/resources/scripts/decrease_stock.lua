-- 재고 차감 Lua 스크립트
-- KEYS[1]: 재고 키 (stock:{productId})
-- ARGV[1]: 차감량
-- ARGV[2]: TTL (초)
-- 반환: 성공 시 차감 후 재고, 실패 시 -1

local key = KEYS[1]
local amount = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

-- 현재 재고 조회
local current = tonumber(redis.call('GET', key) or 0)

-- 재고 부족/0 체크
if current <= 0 or current < amount then
    return -1
end

-- 차감 및 TTL 설정 (atomic)
local newStock = redis.call('DECRBY', key, amount)
redis.call('EXPIRE', key, ttl)

return newStock
