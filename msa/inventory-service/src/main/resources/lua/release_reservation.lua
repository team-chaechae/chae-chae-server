--[[
예약 해제 Lua 스크립트 (보상 트랜잭션)
예약된 재고를 해제하여 다시 판매 가능하게 함

KEYS:
  1: reserved:{productId} - 예약 재고 합계 키
  2: reservation:{sagaId}:{productId} - 개별 예약 정보 키

ARGV:
  (없음)

RETURN:
  성공: 해제된 수량 (>0)
  없음: 0 (이미 해제되었거나 TTL 만료)
]]--

local reservedKey = KEYS[1]
local reservationKey = KEYS[2]

-- 1. 예약 정보 확인
local reservedQty = redis.call('HGET', reservationKey, 'quantity')
if not reservedQty then
    return 0  -- 예약 없음 (이미 해제되었거나 TTL 만료 - 멱등성)
end

local quantity = tonumber(reservedQty)

-- 2. 예약 재고에서 차감
local currentReserved = tonumber(redis.call('GET', reservedKey) or 0)
if currentReserved >= quantity then
    redis.call('DECRBY', reservedKey, quantity)
else
    -- 예약 재고가 음수가 되지 않도록
    redis.call('SET', reservedKey, 0)
end

-- 3. 예약 정보 삭제
redis.call('DEL', reservationKey)

-- 4. 해제된 수량 반환
return quantity
