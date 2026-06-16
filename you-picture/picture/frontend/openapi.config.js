import { generateService } from '@umijs/openapi'

generateService({
    requestLibPath: "import request from '@/request'",
    // 后端服务提供的 Swagger 接口文档的地址(可以后端启动用f12查看请求)
    schemaPath: 'http://localhost:8080/api/v2/api-docs',
    serversPath: './src',
})