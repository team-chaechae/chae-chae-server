--[[
재고 예약 Lua 스크립트
멱등성 보장 + 원자적 예약 처리

KEYS:
  1: stock:{productId} - 총 재고 키
  2: reserved:{productId} - 예약 재고 합계 키
  3: reservation:{sagaId}:{productId} - 개별 예약 정보 키

ARGV:
  1: quantity - 예약할 수량
  2: salesId - 주문 ID
  3: ttlSeconds - 예약 TTL (초)

RETURN:
  성공: 남은 가용 재고 (>=0)
  실패: -1 (재고 부족)
  중복: -2 (이미 예약됨 - 멱등성)
]]--

local stockKey = KEYS[1]
local reservedKey = KEYS[2]
local reservationKey = KEYS[3]

local quantity = tonumber(ARGV[1])
local salesId = ARGV[2]
local ttlSeconds = tonumber(ARGV[3])

-- 1. 멱등성 체크: 이미 예약이 있으면 기존 결과 반환
local existingReservation = redis.call('HGET', reservationKey, 'quantity')
if existingReservation then
    -- 이미 예약됨 - 가용 재고 계산하여 반환
    local totalStock = tonumber(redis.call('GET', stockKey) or 0)
    local totalReserved = tonumber(redis.call('GET', reservedKey) or 0)
    return totalStock - totalReserved
end

-- 2. 가용 재고 계산 (총 재고 - 예약 재고)
local totalStock = tonumber(redis.call('GET', stockKey) or 0)
local totalReserved = tonumber(redis.call('GET', reservedKey) or 0)
local available = totalStock - totalReserved

-- 3. 재고 부족 체크
if available < quantity then
    return -1  -- 재고 부족
end

-- 4. 예약 생성
-- 4-1. 예약 재고 합계 증가
redis.call('INCRBY', reservedKey, quantity)

-- 4-2. 개별 예약 정보 저장 (Hash)
redis.call('HSET', reservationKey,
    'quantity', quantity,
    'salesId', salesId,
    'createdAt', redis.call('TIME')[1]  -- Unix timestamp
)

-- 4-3. TTL 설정 (자동 만료)
redis.call('EXPIRE', reservationKey, ttlSeconds)

-- 5. 새로운 가용 재고 반환
return available - quantity
