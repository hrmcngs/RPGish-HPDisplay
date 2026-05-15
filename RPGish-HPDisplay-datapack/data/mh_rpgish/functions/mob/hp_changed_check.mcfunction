# HPが変更された時の検知 (TamedWolf除外済み)
execute unless data entity @s {Health:512.0f} run function mh_rpgish:mob/hp_changed
