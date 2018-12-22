## sed
使用环境变量替换模板文件中的变量
```
echo "home={HOME}"|sed -E 's/\{(.+)\}/$\1/g'|envsubst
```