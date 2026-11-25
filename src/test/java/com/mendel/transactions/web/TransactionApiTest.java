package com.mendel.transactions.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
    void sumOfAnUnknownTransactionIsNotFound() throws Exception {
        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingParentIsNotFoundAndIsNotStored() throws Exception {
        putTransaction(11, "{\"amount\": 10000, \"type\": \"shopping\", \"parent_id\": 99}")
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/transactions/sum/11"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cycleIsRejectedAndThePreviousSumStays() throws Exception {
        putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\"}");
        putTransaction(11, "{\"amount\": 10000, \"type\": \"shopping\", \"parent_id\": 10}");

        putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\", \"parent_id\": 11}")
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(jsonPath("$.sum").value(15000));
    }

    @Test
    void missingAmountIsRejected() throws Exception {
        putTransaction(10, "{\"type\": \"cars\"}")
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/transactions/sum/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingTypeIsRejected() throws Exception {
        putTransaction(10, "{\"amount\": 5000}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankTypeIsRejected() throws Exception {
        putTransaction(10, "{\"amount\": 5000, \"type\": \" \"}")
                .andExpect(status().isBadRequest());
    }

    private ResultActions putTransaction(long id, String json) throws Exception {
        return mockMvc.perform(put("/transactions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
