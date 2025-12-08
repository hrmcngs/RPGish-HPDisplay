# Armor Standにデータをセット
# itemの名前をArmor Standにコピー
    data modify entity @s CustomName set from entity @e[type=item,distance=..1,limit=1] Item.tag.display.Name
# 表示時間用スコア設定
    scoreboard players set @s DmgDisplayTime 10
# 一時アイテムを削除
    kill @e[type=item,distance=..1,limit=1]
# タグ整理
    tag @s remove DmgNew
