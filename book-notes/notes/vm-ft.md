主从备份

VMwar FT 是对这一思路的极致的实现

核心思想，通过备份，主机失败了从机可以继续提供服务，从而提供可用性

备份有两种方法：
第一种：状态备份，即直接将整个状态拷贝
第二种：复制状态机