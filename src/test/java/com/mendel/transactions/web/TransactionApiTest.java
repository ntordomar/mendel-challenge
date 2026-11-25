package com.mendel.transactions.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransactionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void specificationExample() throws Exception {
        putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\"}").andExpect(status().isCreated());
        putTransaction(11, "{\"amount\": 10000, \"type\": \"shopping\", \"parent_id\": 10}")
                .andExpect(status().isCreated());
        putTransaction(12, "{\"amount\": 5000, \"type\": \"shopping\", \"parent_id\": 11}")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/transactions/types/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsInAnyOrder(10)));

        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(20000));

        mockMvc.perform(get("/transactions/sum/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(15000));
    }

    @Test
    void creatingATransactionReturnsOk() throws Exception {
        putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void replacingATransactionReturnsOkAndUpdatesTheAmount() throws Exception {
        putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\"}").andExpect(status().isCreated());

        putTransaction(10, "{\"amount\": 7000, \"type\": \"cars\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(7000));
    }

    @Test
    void omittingParentClearsThePreviousParent() throws Exception {
        putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\"}");
        putTransaction(11, "{\"amount\": 10000, \"type\": \"shopping\", \"parent_id\": 10}");
        putTransaction(12, "{\"amount\": 5000, \"type\": \"shopping\", \"parent_id\": 11}");

        putTransaction(11, "{\"amount\": 10000, \"type\": \"shopping\"}")
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(jsonPath("$.sum").value(5000));
        mockMvc.perform(get("/transactions/sum/11"))
                .andExpect(jsonPath("$.sum").value(15000));
    }

    @Test
    void unknownTypeReturnsAnEmptyList() throws Exception {
        mockMvc.perform(get("/transactions/types/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void sumsDecimalAmountsFromTheRequest() throws Exception {
        putTransaction(10, "{\"amount\": 0.1, \"type\": \"cars\"}").andExpect(status().isCreated());
        putTransaction(11, "{\"amount\": 0.2, \"type\": \"cars\", \"parent_id\": 10}").andExpect(status().isCreated());

        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(closeTo(0.3, 0.000001)));
    }

    @Test
    void selfParentIsRejectedAndIsNotStored() throws Exception {
        expectError(
                putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\", \"parent_id\": 10}"),
                422,
                "Unprocessable Content",
                "/transactions/10",
                "Transaction 10 would create a cycle");

        expectError(
                mockMvc.perform(get("/transactions/sum/10")),
                404,
                "Not Found",
                "/transactions/sum/10",
                "Transaction not found: 10");
    }

    @Test
    void sumOfAnUnknownTransactionIsNotFound() throws Exception {
        expectError(
                mockMvc.perform(get("/transactions/sum/10")),
                404,
                "Not Found",
                "/transactions/sum/10",
                "Transaction not found: 10");
    }

    @Test
    void missingParentIsRejectedAndIsNotStored() throws Exception {
        expectError(
                putTransaction(11, "{\"amount\": 10000, \"type\": \"shopping\", \"parent_id\": 99}"),
                422,
                "Unprocessable Content",
                "/transactions/11",
                "Parent transaction not found: 99");

        expectError(
                mockMvc.perform(get("/transactions/sum/11")),
                404,
                "Not Found",
                "/transactions/sum/11",
                "Transaction not found: 11");
    }

    @Test
    void cycleIsRejectedAndThePreviousSumStays() throws Exception {
        putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\"}");
        putTransaction(11, "{\"amount\": 10000, \"type\": \"shopping\", \"parent_id\": 10}");

        expectError(
                putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\", \"parent_id\": 11}"),
                422,
                "Unprocessable Content",
                "/transactions/10",
                "Transaction 10 would create a cycle");

        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(jsonPath("$.sum").value(15000));
    }

    @Test
    void missingAmountIsRejected() throws Exception {
        expectError(
                putTransaction(10, "{\"type\": \"cars\"}"),
                400,
                "Bad Request",
                "/transactions/10",
                "amount must not be null");

        expectError(
                mockMvc.perform(get("/transactions/sum/10")),
                404,
                "Not Found",
                "/transactions/sum/10",
                "Transaction not found: 10");
    }

    @Test
    void missingTypeIsRejected() throws Exception {
        expectError(
                putTransaction(10, "{\"amount\": 5000}"),
                400,
                "Bad Request",
                "/transactions/10",
                "type must not be blank");
    }

    @Test
    void blankTypeIsRejected() throws Exception {
        expectError(
                putTransaction(10, "{\"amount\": 5000, \"type\": \" \"}"),
                400,
                "Bad Request",
                "/transactions/10",
                "type must not be blank");
    }

    private void expectError(ResultActions actions, int status, String error, String path, String message)
            throws Exception {
        actions.andExpect(status().is(status))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.message").value(message));
    }

    private ResultActions putTransaction(long id, String json) throws Exception {
        return mockMvc.perform(put("/transactions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
