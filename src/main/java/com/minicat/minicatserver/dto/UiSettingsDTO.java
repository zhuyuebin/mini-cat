package com.minicat.minicatserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端可消费的 UI 功能开关。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UiSettingsDTO {

    /** 是否允许表格单元格内联编辑 */
    private boolean inlineEditEnabled;
}
