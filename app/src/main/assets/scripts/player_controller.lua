function _ready()
    print("=== PETANI HARVEST MOON AKTIF ===")
    player:setPosition(0.0, 0.0, 0.0)
    player:playAnimation("Idle")
end

function _process(delta)
    player:translate(0.2 * delta, 0.0, 0.0)
    player:rotateY(10.0 * delta)
end
