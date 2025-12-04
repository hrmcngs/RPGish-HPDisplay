### 常時実行function (as @e[type=#mh_rpgish:mobs])

# 飼いならしたオオカミは除外 (MC-193202バグ回避)
    execute if entity @s[type=minecraft:wolf] if data entity @s Owner run tag @s add TamedWolf

# モブ召喚時に初期処理
    execute if entity @s[tag=!Init,tag=!TamedWolf] run function mh_rpgish:mob/init
# HPが変更された時の検知
    execute unless entity @s[tag=TamedWolf] run function mh_rpgish:mob/hp_changed_check
# HP表示時間切れ
    execute unless entity @s[tag=TamedWolf] run function mh_rpgish:mob/reset_check