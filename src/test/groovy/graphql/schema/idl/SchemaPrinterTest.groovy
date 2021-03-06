package graphql.schema.idl

import graphql.Scalars
import graphql.TypeResolutionEnvironment
import graphql.schema.*
import spock.lang.Specification

class SchemaPrinterTest extends Specification {

    def nonNull(GraphQLType type) {
        new GraphQLNonNull(type)
    }

    def list(GraphQLType type) {
        new GraphQLList(type)
    }

    GraphQLSchema starWarsSchema() {
        def wiring =  RuntimeWiring.newRuntimeWiring()
                .type("Character", { type -> type.typeResolver(resolver)})
                .scalar(ASTEROID)
                .build()
        GraphQLSchema schema = load("starWarsSchemaExtended.graphqls", wiring)
        schema
    }


    GraphQLScalarType ASTEROID = new GraphQLScalarType("Asteroid","desc", new Coercing() {
        @Override
        Object serialize(Object input) {
            throw new UnsupportedOperationException("Not implemented")
        }

        @Override
        Object parseValue(Object input) {
            throw new UnsupportedOperationException("Not implemented")
        }

        @Override
        Object parseLiteral(Object input) {
            throw new UnsupportedOperationException("Not implemented")
        }
    })

    def resolver = new TypeResolver() {

        @Override
        GraphQLObjectType getType(TypeResolutionEnvironment env) {
            throw new UnsupportedOperationException("Not implemented")
        }
    }

    GraphQLSchema load(String fileName, RuntimeWiring wiring) {
        def stream = getClass().getClassLoader().getResourceAsStream(fileName)

        def typeRegistry = new SchemaParser().parse(new InputStreamReader(stream))
        def schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring)
        schema
    }

    def "typeString"() {

        GraphQLType type1 = nonNull(list(nonNull(list(nonNull(Scalars.GraphQLInt)))))
        GraphQLType type2 = nonNull(nonNull(list(nonNull(Scalars.GraphQLInt))))

        def typeStr1 = new SchemaPrinter().typeString(type1)
        def typeStr2 = new SchemaPrinter().typeString(type2)

        expect:
        typeStr1 == "[[Int!]!]!"
        typeStr2 == "[Int!]!!"

    }

    def "argsString"() {
        def argument1 = new GraphQLArgument("arg1", "desc-arg1", list(nonNull(Scalars.GraphQLInt)), 10)
        def argument2 = new GraphQLArgument("arg2", "desc-arg2", Scalars.GraphQLString, null)
        def argument3 = new GraphQLArgument("arg3", "desc-arg3", Scalars.GraphQLString, "default")
        def argStr = new SchemaPrinter().argsString([argument1, argument2, argument3])

        expect:

        argStr == "(arg1 : [Int!] = 10, arg2 : String, arg3 : String = \"default\")"
    }

    def "print type direct"() {
        GraphQLSchema schema = starWarsSchema()

        def result = new SchemaPrinter().print(schema.getType("Character"))

        expect:
        result ==
                """interface Character {
   id : ID!
   name : String!
   friends : [Character]
   appearsIn : [Episode]!
}

"""
    }

    def "starWars default Test"() {
        GraphQLSchema schema = starWarsSchema()

        def result = new SchemaPrinter().print(schema)

        expect:
        result != null
        !result.contains("scalar")
        !result.contains("__TypeKind")
    }

    def "starWars non default Test"() {
        GraphQLSchema schema = starWarsSchema()

        def options = SchemaPrinter.Options.defaultOptions()
                .includeIntrospectionTypes(true)
                .includeScalarTypes(true)

        def result = new SchemaPrinter(options).print(schema)

        expect:
        result != null
        result.contains("scalar")
        result.contains("__TypeKind")
    }
}
