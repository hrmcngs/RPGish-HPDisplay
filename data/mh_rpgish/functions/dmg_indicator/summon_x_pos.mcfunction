# X正方向
execute if predicate mh_rpgish:random_25 run summon armor_stand ~ ~ ~ {Invisible:1b,NoGravity:0b,CustomNameVisible:1b,Motion:[0.08,0.2,0.04],Tags:["DmgDisplay","DmgNew"]}
execute unless predicate mh_rpgish:random_25 run summon armor_stand ~ ~ ~ {Invisible:1b,NoGravity:0b,CustomNameVisible:1b,Motion:[0.08,0.2,-0.04],Tags:["DmgDisplay","DmgNew"]}
