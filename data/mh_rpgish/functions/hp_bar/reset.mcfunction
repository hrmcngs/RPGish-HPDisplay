# HPバー表示名前のリセット
    data modify entity @s CustomName set value ""
    data modify entity @s CustomNameVisible set value 0b
# もし元々の名前があれば戻す
    execute if entity @s[tag=HasOriginalName] run data modify entity @s CustomName set from entity @s ArmorItems[3].tag.OriginalName
    execute if entity @s[tag=HasOriginalName] run data modify entity @s CustomNameVisible set value 1b