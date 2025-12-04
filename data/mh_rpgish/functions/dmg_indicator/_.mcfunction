# ダメージ表示値
    scoreboard players remove $Health Temporary 512
# モブの高さごとの表示位置の調整
    execute if entity @s[type=#mh_rpgish:size/short] positioned ~ ~0.9 ~ run function mh_rpgish:dmg_indicator/summon
    execute if entity @s[type=#mh_rpgish:size/medium] positioned ~ ~1.3 ~ run function mh_rpgish:dmg_indicator/summon
    execute if entity @s[type=#mh_rpgish:size/human] positioned ~ ~1.8 ~ run function mh_rpgish:dmg_indicator/summon
    execute if entity @s[type=#mh_rpgish:size/tall] positioned ~ ~2.4 ~ run function mh_rpgish:dmg_indicator/summon
# リセット
    scoreboard players reset $DmgColor
# mod属性ダメージタグをクリア
    tag @s remove mh_rpgish.ice_damage
    tag @s remove mh_rpgish.electric_damage
    tag @s remove mh_rpgish.corrosion_damage
    tag @s remove mh_rpgish.holy_damage