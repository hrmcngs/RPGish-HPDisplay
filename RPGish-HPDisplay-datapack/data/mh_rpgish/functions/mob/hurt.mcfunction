# 被ダメージ
    scoreboard players operation $Damage Temporary = $Health Temporary
    scoreboard players remove $Damage Temporary 512
# ダメージ分のスコア減算
    scoreboard players operation @s mh.hp += $Damage Temporary
# 512以上のダメージ検知 (Healthが極端に低い場合、HP<=512のモブは即死)
    execute if score $Health Temporary matches ..10 if score @s mh.hp matches ..512 run scoreboard players set @s mh.hp 0
# Healthを元に戻す
    data modify entity @s Health set value 512.0f
# HP表示
    function mh_rpgish:hp_bar/apply/_
# ダメージ表示
    function mh_rpgish:dmg_indicator/_
# 毒エフェクトでHP<=0の場合、HP=1に保持 (バニラの毒の挙動)
    execute if entity @s[nbt={ActiveEffects:[{Id:19b}]}] if score @s mh.hp matches ..0 run scoreboard players set @s mh.hp 1
# mh.hp<=0で死亡処理 (毒以外)
    execute unless entity @s[nbt={ActiveEffects:[{Id:19b}]}] if score @s mh.hp matches ..0 run kill @s