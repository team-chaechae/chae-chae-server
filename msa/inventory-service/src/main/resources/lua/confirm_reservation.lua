--[[
재고 확정 Lua 스크립트
예약된 재고를 실제 차감하고 예약 해제

KEYS:
  1: stock:{productId} - 총 재고 키
  2: reserved:{productId} - 예약 재고 합계 키
  3: reservation:{sagaId}:{productId} - 개별 예약 정보 키

ARGV:
  (없음)

RETURN:
  성공: 현재 총 재고 (차감 후)
  실패: -1 (예약 없음)
  오류: -2 (예약 수량 불일치)
]]--

local stockKey = KEYS[1]
local reservedKey = KEYS[2]
local reservationKey = KEYS[3]

-- 1. 예약 정보 확인
local reservedQty = redis.call('HGET', reservationKey, 'quantity')
if not reservedQty then
    return -1  -- 예약 없음 (이미 확정되었거나 TTL 만료)
end

local quantity = tonumber(reservedQty)

-- 2. 총 재고에서 차감
local currentStock = tonumber(redis.call('GET', stockKey) or 0)
if currentStock < quantity then
    return -2  -- 재고 불일치 (비정상 상태)
end

local newStock = redis.call('DECRBY', stockKey, quantity)

-- 3. 예약 재고에서 차감
local currentReserved = tonumber(redis.call('GET', reservedKey) or 0)
if currentReserved >= quantity then
    redis.call('DECRBY', reservedKey, quantity)
else
    -- 예약 재고가 음수가 되지 않도록
    redis.call('SET', reservedKey, 0)
end

-- 4. 예약 정보 삭제
redis.call('DEL', reservationKey)

-- 5. 현재 재고 반환
return newStock
