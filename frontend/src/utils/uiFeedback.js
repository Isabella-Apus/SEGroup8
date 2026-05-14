import { ElMessage, ElMessageBox } from "element-plus";
import { standardizeError } from "@/utils/errorStandard";

export const uiMessage = {
  success: (message) => ElMessage.success(message),
  error: (message) => ElMessage.error(standardizeError(message)),
  warning: (message) => ElMessage.warning(message)
};

export const uiDialog = {
  confirm: (message, title, options = {}) => ElMessageBox.confirm(message, title, options),
  prompt: (message, title, options = {}) => ElMessageBox.prompt(message, title, options),
  alert: (message, title, options = {}) => ElMessageBox.alert(message, title, options)
};

