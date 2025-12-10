## 初期処理

# モブのステータス → スコアに
    data modify storage mh_rpgish:temp Init.Health set from entity @s Health
    execute store result score @s mh.hp run data get storage mh_rpgish:temp Init.Health
    execute store result score @s mh.hp_max run data get storage mh_rpgish:temp Init.Health
# 大きなダメージで死なないようHPを1024に設定(スコアでHPを管理)
    data modify entity @s Attributes append value {Name:"minecraft:generic.max_health",Base:1024.0d}
    data modify entity @s Health set value 512.0f
# 元の名前を保存 (名札などで付けられた名前)
    execute if data entity @s CustomName run tag @s add HasOriginalName
    execute if data entity @s CustomName run data modify storage mh_rpgish:temp OriginalName set from entity @s CustomName
    execute if entity @s[tag=HasOriginalName] run data modify entity @s ArmorItems[3].tag.OriginalName set from storage mh_rpgish:temp OriginalName
    data remove storage mh_rpgish:temp OriginalName
# Initタグ付け
    tag @s add Init
# リセット
    data remove storage mh_rpgish:temp Init
