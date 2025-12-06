# ダメージ表示値
    scoreboard players remove $Health Temporary 512
# モブの高さごとの表示位置の調整 (タグでsummon済みかチェック)
    tag @s add mh.need_dmg
    execute if entity @s[type=#mh_rpgish:size/short] positioned ~ ~0.9 ~ run function mh_rpgish:dmg_indicator/summon
    execute if entity @s[type=#mh_rpgish:size/medium] positioned ~ ~1.3 ~ run function mh_rpgish:dmg_indicator/summon
    execute if entity @s[type=#mh_rpgish:size/human] positioned ~ ~1.8 ~ run function mh_rpgish:dmg_indicator/summon
    execute if entity @s[type=#mh_rpgish:size/tall] positioned ~ ~2.4 ~ run function mh_rpgish:dmg_indicator/summon
# どのサイズにも該当しなかった場合のフォールバック
    execute if entity @s[tag=mh.need_dmg] positioned ~ ~1.5 ~ run function mh_rpgish:dmg_indicator/summon
# リセット
    scoreboard players reset $DmgColor
# mod属性ダメージタグをクリア
    tag @s remove minecraft_armor_weapon.mh_rpgish.ice_damage
    tag @s remove minecraft_armor_weapon.mh_rpgish.electric_damage
    tag @s remove minecraft_armor_weapon.mh_rpgish.corrosion_damage
    tag @s remove minecraft_armor_weapon.mh_rpgish.holy_damage