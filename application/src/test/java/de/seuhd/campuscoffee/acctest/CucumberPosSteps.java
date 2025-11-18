package de.seuhd.campuscoffee.acctest;

import de.seuhd.campuscoffee.api.dtos.PosDto;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.PosService;
import io.cucumber.java.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.seuhd.campuscoffee.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the POS Cucumber tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
public class CucumberPosSteps {
    static final PostgreSQLContainer<?> postgresContainer;

    static {
        // share the same testcontainers instance across all Cucumber tests
        postgresContainer = getPostgresContainer();
        postgresContainer.start();
        // testcontainers are automatically stopped when the JVM exits
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        configurePostgresContainers(registry, postgresContainer);
    }

    @Autowired
    protected PosService posService;

    @LocalServerPort
    private Integer port;

    @Before
    public void beforeEach() {
        posService.clear();
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @After
    public void afterEach() {
        posService.clear();
    }

    private List<PosDto> createdPosList;
    private PosDto updatedPos;

    /**
     * Register a Cucumber DataTable type for PosDto.
     * @param row the DataTable row to map to a PosDto object
     * @return the mapped PosDto object
     */
    @DataTableType
    @SuppressWarnings("unused")
    public PosDto toPosDto(Map<String,String> row) {
        return PosDto.builder()
                .name(row.get("name"))
                .description(row.get("description"))
                .type(PosType.valueOf(row.get("type")))
                .campus(CampusType.valueOf(row.get("campus")))
                .street(row.get("street"))
                .houseNumber(row.get("houseNumber"))
                .postalCode(Integer.parseInt(row.get("postalCode")))
                .city(row.get("city"))
                .build();
    }

    // Given -----------------------------------------------------------------------

    @Given("an empty POS list")
    public void anEmptyPosList() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList).isEmpty();
    }

    // TODO: Add Given step for new scenario
    // When -----------------------------------------------------------------------
    @Given( "List with POS Elements")
    public void listWithPosElements(List<PosDto> posList) {
        this.createdPosList = posList;
        assertThat(posList).isNotEmpty();
        //Erstell Liste mit allen POS elementen

    }


    @When("I insert POS with the following elements")
    public void insertPosWithTheFollowingValues(List<PosDto> posList) {
        createdPosList = createPos(posList);
        assertThat(createdPosList).size().isEqualTo(posList.size());
    }

    // TODO: Add When step for new scenario
    // Then -----------------------------------------------------------------------
    @When("I updated the List (descrpition)")
    public void listGotUpdated(List<Map<String,String>> updates) {
        List<PosDto> updatedList= new ArrayList<>();

        for(Map<String,String> i: updates) {
            String name= i.get("name");
            String description= i.get("description");

            PosDto posDtoUpdate = createdPosList.stream() // namen in stream konvertieren
            .filter(pos -> pos.name().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new PosNotFoundException("POS '" + "' nicht gefunden"+ name )); // eigene Exeption werfen

            PosDto updatedPos= posDtoUpdate.toBuilder()
                    .name(name)
                    .description(description)
                    .build();

            updatedList.add(updatedPos);
        }
        createdPosList = updatePos(updatedList); // vorgegebene Funktion nutzen
    }



    @Then("the POS list should contain the same elements in the same order")
    public void thePosListShouldContainTheSameElementsInTheSameOrder() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrderElementsOf(createdPosList);
    }

    // TODO: Add Then step for new scenario
    @Then("cheching if updating worked")
    public void UpdateTheList() {
        List<PosDto> retrievedPosList = retrievePos();

        //erst testen was in Liste geupdated wurde
        for(PosDto i: retrievedPosList) {
            assertThat(i.description())
                    .as("Description not empty")
                    .isNotBlank();
        }

        // Überprüfen ob AKtualisierung erfolgreich hinzugefügt wurde
        assertThat(retrievedPosList)
                .hasSize(createdPosList.size())// es sollen keine Elemente hinzugekommen/ weg sein
                .containsExactlyInAnyOrderElementsOf(createdPosList)
                .as("Updating POS list was successfull")
                .isNotEmpty();
    }
}
