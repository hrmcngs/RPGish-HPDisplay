# ダメージ表示用item召喚
    loot spawn ~ ~ ~ loot mh_rpgish:dmg_indicator
# itemの名前にダメージを代入
    execute as @e[type=item,distance=..0] run function mh_rpgish:dmg_indicator/set_data
# summon済みフラグを削除
    tag @s remove mh.need_dmg