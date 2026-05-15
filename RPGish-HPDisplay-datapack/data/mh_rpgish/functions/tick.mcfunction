### 常時実行function

# 全てのモブとしての常時実行function
    execute as @e[type=#mh_rpgish:mobs] at @s run function mh_rpgish:mob/_
# ダメージ表示用Armor Standの削除 (表示時間終了後)
    scoreboard players remove @e[type=armor_stand,tag=DmgDisplay] DmgDisplayTime 1
    kill @e[type=armor_stand,tag=DmgDisplay,scores={DmgDisplayTime=..0}]
