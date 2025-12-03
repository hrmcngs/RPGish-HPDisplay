# ダメージ表示用データのセット
    # 拾えないように
        data modify entity @s PickupDelay set value 32767
    # 表示時間
        data modify entity @s PortalCooldown set value 10
    # 名前表示
        data modify entity @s CustomNameVisible set value 1b

    # 透明化

        data merge entity @s {Invisible:1}
    # 名前=値
        data modify entity @s CustomName set from entity @s Item.tag.display.Name
# タグ付け
    tag @s add DmgDisplay
