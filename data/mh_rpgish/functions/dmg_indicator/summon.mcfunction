# ダメージ表示用Armor Stand召喚（上に飛んで落ちる動き）
    summon armor_stand ~ ~ ~ {Invisible:1b,NoGravity:0b,CustomNameVisible:1b,Motion:[0.0,0.2,0.0],Tags:["DmgDisplay","DmgNew"]}
# ダメージ表示用item召喚（名前取得用）
    loot spawn ~ ~ ~ loot mh_rpgish:dmg_indicator
# itemの名前をArmor Standにコピー
    execute as @e[type=armor_stand,tag=DmgNew,distance=..1,limit=1] run function mh_rpgish:dmg_indicator/set_data
# summon済みフラグを削除
    tag @s remove mh.need_dmg
