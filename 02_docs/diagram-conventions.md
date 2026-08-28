# 图模型规范

本规范参照课程参考材料的图模型分层，适用于当前 `UC01`–`UC25`，不回写历史归档。

| 层级 | 文件 | 编号 | 规范 |
|---|---|---|---|
| 系统用例 | `diagrams/system-use-case.mmd` | `UCxx` | 参与者位于系统边界外；椭圆只表示完整业务目标；关联线连接参与者与用例；仅在确有复用或可选行为时使用 include/extend |
| 系统行为 | `UCxx/system.mmd` | `SYS-BEHxx` | 系统视为黑盒；只出现参与者与系统，或业务状态/活动，不泄露 Controller、表或框架类 |
| 概念模型 | `UCxx/concept.mmd` | `CONCEPT-CLASSxx` | 只描述业务概念、关联和多重性，不出现 Controller、Service、Mapper、DTO |
| 组件结构 | `UCxx/component.mmd` | `COMP-STRUCTxx` | 组件有明确职责；依赖方向与调用方向一致；连线标注接口或协作语义 |
| 组件顺序 | `UCxx/component-sequence.mmd` | `COMP-SEQxx` | 从边界组件进入控制/服务/数据或外部组件；调用与返回方向清楚；顺序自动编号 |
| 详细类图 | `UCxx/object.mmd` | `DESIGN-CLASSxx` | 展示实现类、职责构造型与依赖；与当前代码命名一致，不把目标服务冒充当前类 |
| 对象顺序 | `UCxx/object-sequence.mmd` | `OBJ-SEQxx` | 使用实现对象或类实例；消息对应方法/业务操作；顺序自动编号，并保留分支或循环 |

图与用例说明采用同一组 `REQxx / USxx / UCxx / ACxx-yyy` 标识；结构图表达静态职责，顺序图表达一次场景交互，两者不能互相替代。
